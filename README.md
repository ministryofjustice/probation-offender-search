# probation-offender-search

[![CircleCI](https://circleci.com/gh/ministryofjustice/probation-offender-search/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/probation-offender-search)
[![Docker](https://github.com/orgs/ministryofjustice/packages?repo_name=probation-offender-search)](https://github.com/orgs/ministryofjustice/packages?repo_name=probation-offender-search)
[![API docs](https://img.shields.io/badge/API_docs_(needs_VPN)-view-85EA2D.svg?logo=swagger)](https://probation-offender-search-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html)

API to provides searching of offender records in Delius via Elastic search

## Intellij setup

- Install JDK 21
- Enable Gradle using JDK 21
- Set JDK in project structure
- Enable the lombok plugin and restart if necessary
- Enable Annotation Processors at "Settings > Build > Compiler > Annotation Processors"

#### Health

- `/health/ping`: will respond `{ status: "UP" }` to all requests.  This should be used by dependent systems to check connectivity to the offender search service.
- `/health`: provides information about the application health and its dependencies.  This should be used the notm-montor & PagerDuty.
- `/info`: provides information about the version of the deployed application, used by the notm-monitor.

## Running OpenSearch for tests
The integration tests use the 'test' profile and require an [OpenSearch docker container](https://opensearch.org/docs/latest/install-and-configure/install-opensearch/docker/):
```bash
$ docker-compose pull opensearch
$ docker-compose up opensearch
```

Running the test `uk.gov.justice.hmpps.offendersearch.controllers.OffenderSearchAPIIntegrationTest` will insert an Offender Index on 
the locally running OpenSearch instance and populate with Offender test data. OpenSearch has been added to the CircleCI configuration for 
the build pipeline too. See [config.yml](./.circleci/config.yml).

#### Building & Running

Note the OpenSearch container is required for both tests and local running.

```$bash
$ ./gradlew test
$ SPRING_PROFILES_ACTIVE=dev,opensearch ./gradlew bootRun
```
