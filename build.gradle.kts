import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "11.0.6"
  kotlin("plugin.spring") version "2.4.10"
}

dependencies {
  constraints {
    implementation("org.eclipse.parsson:parsson:1.1.9") {
      because("Fix CVE-2026-9563 found in 1.1.7")
    }
  }

  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-reactor-netty")
  implementation("org.springframework.data:spring-data-jpa")
  implementation("io.jsonwebtoken:jjwt-impl:0.13.0")
  implementation("io.jsonwebtoken:jjwt-jackson:0.13.0")
  implementation("org.opensearch.client:spring-data-opensearch-starter:3.1.1")
  implementation("org.opensearch.client:opensearch-java:3.9.0")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.0")
  implementation("io.sentry:sentry-spring-boot-4:8.53.0")
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:3.0.0")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:7.4.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.31.0")
  implementation("io.opentelemetry:opentelemetry-extension-kotlin")
  implementation("io.flipt:flipt-client-java:1.3.3") {
    exclude("org.apache.httpcomponents", "httpclient")
  }

  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.junit.vintage:junit-vintage-engine")
  testImplementation("org.wiremock:wiremock-standalone:3.13.2")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:6.2.0")
  testImplementation("org.awaitility:awaitility-kotlin")
  testImplementation("io.rest-assured:json-path:6.0.1")
  testImplementation("io.rest-assured:xml-path:6.0.1")
  testImplementation("io.rest-assured:spring-mock-mvc:6.0.1")
  testImplementation("io.swagger.parser.v3:swagger-parser-v3:2.1.47")
  testImplementation("org.opensearch:opensearch-testcontainers:4.1.0")
  testImplementation("org.testcontainers:testcontainers-junit-jupiter:2.0.5")
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(25))
kotlin.compilerOptions.jvmTarget.set(JvmTarget.JVM_25)

// Disable ktlint in favour of IntelliJ formatting
configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
  filter {
    exclude("**/*")
  }
}
