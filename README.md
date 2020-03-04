# offender-search
API to provides searching of offender records in Delius via Elastic search

##Intellij setup

- Install jdk 11
- Enable Gradle using jdk 11
- Set jdk in project structure
- Enable the lombok plugin and restart if necessary
- Enable Annotation Processors at "Settings > Build > Compiler > Annotation Processors"

#### Health

- `/health/ping`: will respond `pong` to all requests.  This should be used by dependent systems to check connectivity to the offender search service.
- `/health`: provides information about the application health and its dependencies.  This should only be used the notm-montor & PagerDuty.
- `/info`: provides information about the version of the deployed application, used by the notm-monitor.

## Running localstack to provide elasticSearch for tests
The integration tests use the 'localstack' profile and require a localstack docker container:
```bash
$ docker-compose pull localstack
$ TMPDIR=/tmp docker-compose up localstack
```

Running the test `uk.gov.justice.hmpps.offendersearch.controllers.OffenderSearchAPIIntegrationTest` will insert an Offender Index on 
the locally running LocalStack instance and populate with Offender test data. Localstack has been added to the CircleCI configuration for 
the build pipeline too.

#### Building & Running

Note the required localstack container for both tests and local running.

```$bash
$ ./gradlew clean install test
$ ./gradlew bootRun --args='--spring.profiles.active=dev,localstack'
```
