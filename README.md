# probation-offender-search

[![CircleCI](https://circleci.com/gh/ministryofjustice/probation-offender-search/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/probation-offender-search)
[![Docker](https://quay.io/repository/hmpps/probation-offender-search/status)](https://quay.io/repository/hmpps/probation-offender-search/status)
[![API docs](https://img.shields.io/badge/API_docs_(needs_VPN)-view-85EA2D.svg?logo=swagger)](https://probation-offender-search-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html)

API to provides searching of offender records in Delius via Elastic search

##Intellij setup

- Install jdk 11
- Enable Gradle using jdk 11
- Set jdk in project structure
- Enable the lombok plugin and restart if necessary
- Enable Annotation Processors at "Settings > Build > Compiler > Annotation Processors"

#### Health

- `/health/ping`: will respond `{ status: "UP" }` to all requests.  This should be used by dependent systems to check connectivity to the offender search service.
- `/health`: provides information about the application health and its dependencies.  This should be used the notm-montor & PagerDuty.
- `/info`: provides information about the version of the deployed application, used by the notm-monitor.

## Running elasticSearch for tests
The integration tests use the 'test' profile and require an elasticsearch docker container:
```bash
$ docker-compose pull docker.elastic.co/elasticsearch/elasticsearch:7.10.2
$ TMPDIR=/tmp docker-compose up elasticsearch
```

Running the test `uk.gov.justice.hmpps.offendersearch.controllers.OffenderSearchAPIIntegrationTest` will insert an Offender Index on 
the locally running Elasticsearch instance and populate with Offender test data. ES has been added to the CircleCI configuration for 
the build pipeline too.

#### Building & Running

Note the required Elasticsearch container for both tests and local running.

```$bash
$ ./gradlew clean install test
$ ./gradlew bootRun --args='--spring.profiles.active=dev,elasticsearch'
```
