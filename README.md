# probation-offender-search

[![CircleCI](https://circleci.com/gh/ministryofjustice/probation-offender-search/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/probation-offender-search)
[![Docker](https://github.com/orgs/ministryofjustice/packages?repo_name=probation-offender-search)](https://github.com/orgs/ministryofjustice/packages?repo_name=probation-offender-search)
[![API docs](https://img.shields.io/badge/API_docs_(needs_VPN)-view-85EA2D.svg?logo=swagger)](https://probation-offender-search-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html)

API to provides searching of offender records in Delius via Elastic search

## IntelliJ setup

- Install JDK 25
- Enable Gradle using JDK 25
- Set JDK in project structure
- Enable the lombok plugin and restart if necessary
- Enable Annotation Processors at "Settings > Build > Compiler > Annotation Processors"

#### Health

- `/health/ping`: will respond `{ status: "UP" }` to all requests.  This should be used by dependent systems to check connectivity to the offender search service.
- `/health`: provides information about the application health and its dependencies.  This should be used the notm-montor & PagerDuty.
- `/info`: provides information about the version of the deployed application, used by the notm-monitor.

## Running OpenSearch for tests

The integration tests use the 'test' profile and requires Docker to be running. Testcontainers will start the required OpenSearch container.
[OpenSearchSetup.kt](src/test/kotlin/uk/gov/justice/hmpps/probationsearch/contactsearch/OpenSearchSetup.kt) will populate the container with configuration and test data.

#### Building & Running

To run locally, you can start up the container using Docker compose.

```bash
docker-compose pull opensearch
docker-compose up opensearch
```

```bash
SPRING_PROFILES_ACTIVE=dev,opensearch ./gradlew bootRun
```
