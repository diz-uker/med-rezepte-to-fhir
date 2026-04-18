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
