#!/usr/bin/env python3
"""Verifies the FHIR bundles produced by the app during the e2e test.

Reads the fixtures used to seed the input topic (src/test/resources/fixtures)
to compute which of them the mapper is expected to turn into a FHIR bundle
(see MedRezeptToFhirBundleMapper.map's blank-field checks), then checks the
messages consumed from the output/provenance/DLQ topics against that.

Input files are produced by `kafka-console-consumer.sh --property
print.key=true --property key.separator='|'`, i.e. lines of `key|value-json`
(plus occasional blank lines).
"""

import argparse
import json
import sys
from collections import Counter
from pathlib import Path


def is_blank(value):
    return value is None or (isinstance(value, str) and value.strip() == "")


def expected_identifiers(fixtures_dir):
    identifiers = Counter()
    skipped = []
    for path in sorted(Path(fixtures_dir).glob("rezept-*.json")):
        record = json.loads(path.read_text())
        if (
            is_blank(record.get("REZEPT_ID"))
            or is_blank(record.get("REZEPT_POS"))
            or is_blank(record.get("VERSCHREIBUNG"))
        ):
            skipped.append(path.name)
            continue
        identifiers[f"{record['REZEPT_ID']}-{record['REZEPT_POS']}"] += 1
    return identifiers, skipped


def read_messages(path):
    messages = []
    for line in Path(path).read_text().splitlines():
        if "|" not in line:
            continue
        key, _, value = line.partition("|")
        value = value.strip()
        if not value:
            continue
        try:
            messages.append((key, json.loads(value)))
        except json.JSONDecodeError as e:
            raise AssertionError(f"non-JSON message value in {path}: {value!r}") from e
    return messages


def find_entry(bundle, resource_type):
    for entry in bundle.get("entry", []):
        resource = entry.get("resource", {})
        if resource.get("resourceType") == resource_type:
            return resource
    return None


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--fixtures-dir", required=True)
    parser.add_argument("--data-file", required=True)
    parser.add_argument("--provenance-file", required=True)
    parser.add_argument("--dlq-file", required=True)
    args = parser.parse_args()

    errors = []

    expected, skipped_fixtures = expected_identifiers(args.fixtures_dir)
    expected_total = sum(expected.values())
    print(
        f"expecting {expected_total} mapped bundle(s) from fixtures; "
        "skipped (blank rezeptId/rezeptPos/verschreibung): "
        f"{skipped_fixtures or 'none'}"
    )

    dlq_messages = read_messages(args.dlq_file)
    if dlq_messages:
        errors.append(
            f"expected an empty DLQ topic, found {len(dlq_messages)} message(s)"
        )

    data_messages = read_messages(args.data_file)
    if len(data_messages) != expected_total:
        errors.append(
            f"expected {expected_total} message(s) on the data output "
            f"topic, got {len(data_messages)}"
        )

    actual_identifiers = Counter()
    data_keys = Counter()
    for key, bundle in data_messages:
        data_keys[key] += 1
        if (
            bundle.get("resourceType") != "Bundle"
            or bundle.get("type") != "transaction"
        ):
            errors.append(
                f"data message {key} is not a transaction Bundle: "
                f"{bundle.get('resourceType')}/{bundle.get('type')}"
            )
            continue
        if bundle.get("id") != key:
            errors.append(
                f"data message key {key} does not match bundle id {bundle.get('id')}"
            )
        med_request = find_entry(bundle, "MedicationRequest")
        if med_request is None:
            errors.append(f"data message {key} has no MedicationRequest entry")
            continue
        if find_entry(bundle, "Medication") is None:
            errors.append(f"data message {key} has no Medication entry")
        identifiers = med_request.get("identifier", [])
        if not identifiers:
            errors.append(f"data message {key} MedicationRequest has no identifier")
            continue
        actual_identifiers[identifiers[0]["value"]] += 1

    if actual_identifiers != expected:
        errors.append(
            "mapped identifiers don't match fixtures.\n"
            f"  expected: {sorted(expected.elements())}\n"
            f"  actual:   {sorted(actual_identifiers.elements())}"
        )

    provenance_messages = read_messages(args.provenance_file)
    if len(provenance_messages) != expected_total:
        errors.append(
            f"expected {expected_total} message(s) on the provenance "
            f"output topic, got {len(provenance_messages)}"
        )

    provenance_keys = Counter()
    for key, bundle in provenance_messages:
        if not key.startswith("provenance-"):
            errors.append(
                f"provenance message key {key!r} missing 'provenance-' prefix"
            )
            continue
        provenance_keys[key[len("provenance-") :]] += 1
        if (
            bundle.get("resourceType") != "Bundle"
            or bundle.get("type") != "transaction"
        ):
            errors.append(f"provenance message {key} is not a transaction Bundle")
            continue
        if find_entry(bundle, "Provenance") is None:
            errors.append(f"provenance message {key} has no Provenance entry")
        if find_entry(bundle, "Device") is None:
            errors.append(f"provenance message {key} has no Device entry")

    if provenance_keys != data_keys:
        errors.append(
            "provenance message keys don't correspond 1:1 to data "
            "message keys.\n"
            f"  data keys:       {sorted(data_keys.elements())}\n"
            f"  provenance keys: {sorted(provenance_keys.elements())}"
        )

    if errors:
        print("\nFAILED:", file=sys.stderr)
        for error in errors:
            print(f"  - {error}", file=sys.stderr)
        sys.exit(1)

    print(f"OK: {expected_total} bundle(s) with matching provenance and an empty DLQ")


if __name__ == "__main__":
    main()
