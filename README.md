# med-rezepte-to-fhir

Map SAP-post-processed medication prescription records to FHIR® resources.

## Snapshot testing

The project makes extensive use of snapshot testing to verify the created FHIR
resources and avoid regressions.
We use <https://github.com/approvals/ApprovalTests.Java> for this.

### Approving changes automatically

Usually, approving a changed snapshots requires manually renaming or moving the
snapshot file from `.received.` to `.approved.`.
If you are facing a lot of changed snapshots and are certain that your changes
are valid, you can automatically approve them:

```sh
APPROVAL_TESTS_USE_REPORTER=AutoApproveReporter ./gradlew test
```

Source: <https://github.com/approvals/ApprovalTests.Java/issues/590>.

You can also run this in a loop to approve indexed snapshots:

```sh
for i in {1..10}; do
    APPROVAL_TESTS_USE_REPORTER=AutoApproveReporter ./gradlew test;
done
```

## End-to-end testing

`hack/e2e/run.sh` runs a full end-to-end test in a local
[kind](https://kind.sigs.k8s.io/) cluster: it installs the
[Strimzi Kafka operator](https://strimzi.io/) and a Kafka cluster, deploys
this app via the
[miracum/charts `stream-processors`](https://github.com/miracum/charts/tree/master/charts/stream-processors)
chart, produces the fixtures from `src/test/resources/fixtures` onto the
input topic, and asserts that the expected FHIR bundles show up on the
output/provenance topics with an empty dead-letter-queue topic. Requires
`docker`, `kind`, `kubectl` and `helm`:

```sh
./hack/e2e/run.sh
```

See the comment header in that script for configuration environment
variables (e.g. `SKIP_KIND_CREATE`/`SKIP_BUILD` to reuse an existing cluster
or image, or `KEEP_CLUSTER` to leave the cluster running for debugging).
