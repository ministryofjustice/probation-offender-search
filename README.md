# offender-search
Provides searching of Offender records in Delius via Elastic search

## Running localstack to provide elasticSearch
The integration tests that use the localstack profile require a localstack docker container:
```bash
TMPDIR=/private$TMPDIR docker-compose up localstack
```
Running the test `uk.gov.justice.hmpps.offendersearch.controllers.OffenderSearchAPIIntegrationTest` will insert an Offender Index on the locally running LocalStack instance and populate
with Offender test data.
Localstack has been added to the CircleCI configuration for the build pipeline.
