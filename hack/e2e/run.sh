#!/usr/bin/env bash
# End-to-end test:
#   1. spins up a kind cluster (unless SKIP_KIND_CREATE=true, e.g. when the
#      caller already created one, as helm/kind-action does in CI)
#   2. installs the Strimzi Kafka operator and a single-node Kafka cluster
#      (hack/e2e/manifests/kafka.yaml)
#   3. builds this app's container image and loads it into the cluster
#   4. installs this app via the miracum/charts stream-processors chart
#      (https://github.com/miracum/charts/tree/master/charts/stream-processors)
#   5. produces the test fixtures (src/test/resources/fixtures) onto the
#      input topic
#   6. consumes the output/provenance/DLQ topics and counts the produced
#      messages and FHIR resources. Which records are mapped or skipped, and
#      what the bundles contain, is covered by the unit/snapshot tests; this
#      only checks that the deployed app is wired up correctly.
#
# Usage: hack/e2e/run.sh
#
# Env vars (all optional):
#   CLUSTER_NAME        kind cluster name                 (default: med-rezepte-to-fhir-e2e)
#   NAMESPACE           k8s namespace for everything       (default: kafka)
#   IMAGE                app image ref to build/load        (default: med-rezepte-to-fhir:e2e-test)
#   STRIMZI_CHART_VERSION       Strimzi operator helm chart version (default: 1.1.0)
#   STREAM_PROCESSORS_CHART_VERSION  stream-processors chart version (default: 2.0.0)
#   SKIP_KIND_CREATE    "true" to reuse the current kube context instead of creating a kind cluster
#   SKIP_BUILD          "true" to reuse an already-built $IMAGE instead of rebuilding it
#   KEEP_CLUSTER         "true" to leave the kind cluster running after the test (implies SKIP_KIND_CREATE was false)
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
E2E_DIR="${ROOT_DIR}/hack/e2e"
WORK_DIR="$(mktemp -d)"

CLUSTER_NAME="${CLUSTER_NAME:-med-rezepte-to-fhir-e2e}"
NAMESPACE="${NAMESPACE:-kafka}"
IMAGE="${IMAGE:-med-rezepte-to-fhir:e2e-test}"
STRIMZI_CHART_VERSION="${STRIMZI_CHART_VERSION:-1.1.0}"
STREAM_PROCESSORS_CHART_VERSION="${STREAM_PROCESSORS_CHART_VERSION:-2.0.0}"
SKIP_KIND_CREATE="${SKIP_KIND_CREATE:-false}"
SKIP_BUILD="${SKIP_BUILD:-false}"
KEEP_CLUSTER="${KEEP_CLUSTER:-false}"

RELEASE_NAME="medrezept-to-fhir"
PROCESSOR_NAME="medrezept-to-fhir"
INPUT_TOPIC="dwh.med-rezepte"
OUTPUT_TOPIC="fhir.med-rezepte"
PROVENANCE_TOPIC="fhir.med-rezepte.provenance"
DLQ_TOPIC="error.${INPUT_TOPIC}.${PROCESSOR_NAME}"
# number of fixtures in src/test/resources/fixtures expected to be mapped to
# a bundle, the others are skipped by the app. Update when adding fixtures.
EXPECTED_BUNDLES=12

log() { echo -e "\033[1;34m[e2e]\033[0m $*"; }

cleanup() {
  local status=$?
  if [[ "${status}" -ne 0 ]]; then
    log "test failed, dumping cluster state for debugging"
    kubectl get all,kafka,kafkanodepool,kafkauser -n "${NAMESPACE}" -o wide || true
    kubectl logs -n "${NAMESPACE}" -l "app.kubernetes.io/name=${PROCESSOR_NAME}" --tail=500 || true
  fi
  if [[ "${SKIP_KIND_CREATE}" != "true" && "${KEEP_CLUSTER}" != "true" ]]; then
    log "deleting kind cluster ${CLUSTER_NAME}"
    kind delete cluster --name "${CLUSTER_NAME}" || true
  fi
  rm -rf "${WORK_DIR}"
  exit "${status}"
}
trap cleanup EXIT

if [[ "${SKIP_KIND_CREATE}" != "true" ]]; then
  log "creating kind cluster ${CLUSTER_NAME}"
  kind create cluster --name "${CLUSTER_NAME}" --wait 120s
fi

if [[ "${SKIP_BUILD}" != "true" ]]; then
  log "building app image ${IMAGE}"
  docker build -t "${IMAGE}" "${ROOT_DIR}"
fi

log "loading ${IMAGE} into kind cluster"
kind load docker-image "${IMAGE}" --name "${CLUSTER_NAME}"

log "installing Strimzi Kafka operator ${STRIMZI_CHART_VERSION}"
helm upgrade --install strimzi-operator oci://quay.io/strimzi-helm/strimzi-kafka-operator \
  --version "${STRIMZI_CHART_VERSION}" \
  --namespace "${NAMESPACE}" --create-namespace \
  --wait --timeout 5m

log "applying Kafka cluster manifests"
kubectl apply -n "${NAMESPACE}" -f "${E2E_DIR}/manifests/kafka.yaml"

log "waiting for Kafka cluster to become ready"
kubectl wait --for=condition=Ready "kafka/my-cluster" -n "${NAMESPACE}" --timeout=300s

IMAGE_REGISTRY="${IMAGE%%/*}"
IMAGE_REST="${IMAGE#*/}"
if [[ "${IMAGE_REGISTRY}" == "${IMAGE}" ]]; then
  # no registry/namespace prefix in $IMAGE, e.g. "med-rezepte-to-fhir:e2e-test"
  IMAGE_REGISTRY="docker.io/library"
  IMAGE_REST="${IMAGE}"
fi
IMAGE_REPOSITORY="${IMAGE_REST%%:*}"
IMAGE_TAG="${IMAGE_REST##*:}"

log "installing stream-processors chart ${STREAM_PROCESSORS_CHART_VERSION} (release ${RELEASE_NAME})"
helm upgrade --install "${RELEASE_NAME}" oci://ghcr.io/miracum/charts/stream-processors \
  --version "${STREAM_PROCESSORS_CHART_VERSION}" \
  --namespace "${NAMESPACE}" \
  -f "${E2E_DIR}/values.yaml" \
  --set "processors.${PROCESSOR_NAME}.container.image.registry=${IMAGE_REGISTRY}" \
  --set "processors.${PROCESSOR_NAME}.container.image.repository=${IMAGE_REPOSITORY}" \
  --set "processors.${PROCESSOR_NAME}.container.image.tag=${IMAGE_TAG}" \
  --wait --timeout 3m

log "waiting for the app to finish starting up and its consumer group to be assigned partitions"
POD_SELECTOR="app.kubernetes.io/name=${PROCESSOR_NAME}"
# The app exposes no HTTP/actuator endpoint (no readiness probe is configured),
# so k8s marks the pod "Ready" as soon as the process starts. We instead poll
# the app's own logs for the markers that show it has (a) finished starting
# and (b) actually been assigned partitions - otherwise a consumer group
# that hasn't rebalanced yet would miss messages produced before it joins
# (the binder uses the Kafka default auto.offset.reset=latest).
for _ in $(seq 1 60); do
  logs="$(kubectl logs -n "${NAMESPACE}" -l "${POD_SELECTOR}" --tail=-1 2>/dev/null || true)"
  if grep -q "Started MedRezepteToFhirApplication" <<<"${logs}" \
    && grep -q "partitions assigned: \[${INPUT_TOPIC}" <<<"${logs}"; then
    log "app is up and consuming from ${INPUT_TOPIC}"
    break
  fi
  sleep 2
done
if ! grep -q "partitions assigned: \[${INPUT_TOPIC}" <<<"${logs}"; then
  log "app never reported partition assignment, dumping logs"
  echo "${logs}"
  exit 1
fi

log "starting Kafka client pod used to seed fixtures and read back results"
kubectl run kafka-client -n "${NAMESPACE}" --image=docker.io/apache/kafka:4.3.0 --restart=Never --command -- sleep 3600
kubectl wait --for=condition=Ready pod/kafka-client -n "${NAMESPACE}" --timeout=60s

FIXTURES_FILE="${WORK_DIR}/fixtures.ndjson"
: >"${FIXTURES_FILE}"
for fixture in "${ROOT_DIR}"/src/test/resources/fixtures/rezept-*.json; do
  jq -c . "${fixture}" >>"${FIXTURES_FILE}"
done
log "seeding $(wc -l <"${FIXTURES_FILE}") fixture record(s) into ${INPUT_TOPIC}"
kubectl cp "${FIXTURES_FILE}" "${NAMESPACE}/kafka-client:/tmp/fixtures.ndjson"
kubectl exec -n "${NAMESPACE}" kafka-client -- bash -c \
  "/opt/kafka/bin/kafka-console-producer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic ${INPUT_TOPIC} < /tmp/fixtures.ndjson"

consume() {
  local topic="$1"
  local out_file="$2"
  kubectl exec -n "${NAMESPACE}" kafka-client -- timeout 30 /opt/kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server my-cluster-kafka-bootstrap:9092 \
    --topic "${topic}" \
    --from-beginning \
    --timeout-ms 15000 \
    --property print.key=true \
    --property key.separator='|' \
    >"${out_file}" 2>/dev/null || true
}

log "consuming ${OUTPUT_TOPIC}, ${PROVENANCE_TOPIC} and ${DLQ_TOPIC}"
consume "${OUTPUT_TOPIC}" "${WORK_DIR}/data.txt"
consume "${PROVENANCE_TOPIC}" "${WORK_DIR}/provenance.txt"
consume "${DLQ_TOPIC}" "${WORK_DIR}/dlq.txt"

# kafka-console-consumer prints one "key|value-json" line per message (plus
# occasional blank lines), turns that into a JSON array of {key, value}
messages() {
  jq -R -s -c '[split("\n")[] | select(contains("|")) | split("|")
    | {key: .[0], value: (.[1:] | join("|") | fromjson)}]' "$1"
}

resource_type_counts() {
  jq -c '[.[].value.entry[].resource.resourceType]
    | group_by(.) | map({key: .[0], value: length}) | from_entries'
}

failed=false
check() {
  local description="$1" expected="$2" actual="$3"
  if [[ "${actual}" == "${expected}" ]]; then
    log "OK: ${description}: ${actual}"
  else
    log "FAILED: ${description}: expected ${expected}, got ${actual}"
    failed=true
  fi
}

log "verifying consumed messages"
data="$(messages "${WORK_DIR}/data.txt")"
provenance="$(messages "${WORK_DIR}/provenance.txt")"
dlq="$(messages "${WORK_DIR}/dlq.txt")"

check "data messages" "${EXPECTED_BUNDLES}" "$(jq length <<<"${data}")"
check "data resources" \
  "{\"Medication\":${EXPECTED_BUNDLES},\"MedicationRequest\":${EXPECTED_BUNDLES}}" \
  "$(resource_type_counts <<<"${data}")"
check "data messages whose key is not the bundle id" 0 \
  "$(jq '[.[] | select(.key != .value.id)] | length' <<<"${data}")"
check "provenance messages" "${EXPECTED_BUNDLES}" "$(jq length <<<"${provenance}")"
check "provenance resources" \
  "{\"Device\":${EXPECTED_BUNDLES},\"Provenance\":${EXPECTED_BUNDLES}}" \
  "$(resource_type_counts <<<"${provenance}")"
check "DLQ messages" 0 "$(jq length <<<"${dlq}")"

if [[ "${failed}" == "true" ]]; then
  exit 1
fi

log "e2e test passed"
