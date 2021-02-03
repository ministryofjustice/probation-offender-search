    {{/* vim: set filetype=mustache: */}}
{{/*
Environment variables for web and worker containers
*/}}
{{- define "deployment.envs" }}
env:
  - name: JAVA_OPTS
    value: "{{ .Values.env.JAVA_OPTS }}"

  - name: SPRING_PROFILES_ACTIVE
    value: "elasticsearch"

  - name: SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI
    value: "{{ .Values.env.SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI }}"

  - name: COMMUNITY_ENDPOINT_URL
    value: "{{ .Values.env.COMMUNITY_ENDPOINT_URL }}"

  - name: APPINSIGHTS_INSTRUMENTATIONKEY
    valueFrom:
      secretKeyRef:
        name: {{ template "app.name" . }}
        key: APPINSIGHTS_INSTRUMENTATIONKEY

  - name: APPLICATIONINSIGHTS_CONNECTION_STRING
    value: "InstrumentationKey=$(APPINSIGHTS_INSTRUMENTATIONKEY)"

  - name: AWS_REGION
    value: "eu-west-2"

  - name: ELASTICSEARCH_PORT
    value: "9200"

  - name: ELASTICSEARCH_SCHEME
    value: "http"

  - name: ELASTICSEARCH_HOST
    value: "es-proxy"

  - name: ELASTICSEARCH_AWS_SIGNREQUESTS
    value: "false"

  - name: ELASTICSEARCH_PROVIDER
    value: "aws"

  - name: SEARCH_SUPPORTED_MAPPING_VERSION
    value: "2"

{{- end -}}
