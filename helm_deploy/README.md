# Deployment Notes

## Prerequisites

- Ensure you have helm v3 client installed.

```sh
$ helm version
version.BuildInfo{Version:"v3.0.1", GitCommit:"7c22ef9ce89e0ebeb7125ba2ebf7d421f3e82ffa", GitTreeState:"clean", GoVersion:"go1.13.4"}
```

- Ensure a TLS cert for your intended hostname is configured and ready, see section below.

### Useful helm (v3) commands:

__Test chart template rendering:__

This will out the fully rendered kubernetes resources in raw yaml.

```sh
helm template [path to chart] --values=values-dev.yaml
```

__List releases:__

```sh
helm --namespace [namespace] list
```

__List current and previously installed application versions:__

```sh
helm --namespace [namespace] history [release name]
```

__Rollback to previous version:__

```sh
helm --namespace [namespace] rollback [release name] [revision number] --wait
```

Note: replace _revision number_ with one from listed in the `history` command)

__Example deploy command:__

The following example is `--dry-run` mode - which will allow for testing. CircleCI normally runs this command with actual secret values (from AWS secret manager), and also updated the chart's application version to match the release version:

```sh
helm upgrade [release name] [path to chart]. \
  --install --wait --force --reset-values --timeout 5m --history-max 10 \
  --dry-run \
  --namespace [namespace] \
  --values values-dev.yaml \
  --values example-secrets.yaml
```

### Ingress TLS certificate

Ensure a certificate definition exists in the cloud-platform-environments repo under the relevant namespaces folder:

e.g.

```sh
cloud-platform-environments/namespaces/live-1.cloud-platform.service.justice.gov.uk/[INSERT NAMESPACE NAME]/05-certificate.yaml
```

Ensure the certificate is created and ready for use.

The name of the kubernetes secret where the certificate is stored is used as a value to the helm chart - this is used to configured the ingress.

# Support

## Custom Alerts

### Synthetic Monitor

There is a Cronjob called `synthetic-monitor` which performs a simple probation offender search every 10 minutes. It then records the number of results and the request duration as telemetry events on Application Insights.

You can see those telemetry events over time with these App Insights log queries:

```kusto
customEvents
| where cloud_RoleName == "probation-offender-search"
| where name == "synthetic-monitor"
| extend timems=toint(customDimensions.timeMs),results=toint(customDimensions.results)
| summarize avg(timems),max(timems) by bin(timestamp, 15m)
| render timechart 
```

```kusto
customEvents
| where cloud_RoleName == "probation-offender-search"
| where name == "synthetic-monitor"
| extend timems=toint(customDimensions.timeMs),results=toint(customDimensions.results)
| summarize avg(results),max(results) by bin(timestamp, 15m)
| render timechart 
```

An alert has been created for each metric in Application Insights.

* `Probation Offender Search - synthetic monitor response time` - checks if the average response time for the search is higher than an arbitrary limit. This indicates that the system is performing slowly and you should investigate the load on the system.
* `Probation Offender Search - synthetic monitor result size` - checks if the number of results returned by the search has dropped below an arbitrary limit. This indicates that either the data in the system has drastically changed or that there is some kind of bug with the search meaning not all results are being found.
