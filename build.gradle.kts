import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "9.0.1"
  kotlin("plugin.spring") version "2.2.10"
  id("com.google.cloud.tools.jib") version "3.4.5"
}

dependencies {
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.data:spring-data-jpa")
  implementation("io.jsonwebtoken:jjwt-impl:0.13.0")
  implementation("io.jsonwebtoken:jjwt-jackson:0.13.0")
  implementation("org.opensearch.client:spring-data-opensearch-starter:2.0.0")
  implementation("org.opensearch.client:opensearch-java:3.2.0")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.11")
  implementation("io.sentry:sentry-spring-boot-starter-jakarta:8.20.0")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.4.10")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.19.0")
  implementation("io.opentelemetry:opentelemetry-extension-kotlin")
  implementation("io.flipt:flipt-client-java:1.1.1") {
    exclude("org.apache.httpcomponents", "httpclient")
  }

  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.junit.vintage:junit-vintage-engine")
  testImplementation("org.wiremock:wiremock-standalone:3.13.1")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:4.1.1")
  testImplementation("org.awaitility:awaitility-kotlin")
  testImplementation("io.rest-assured:json-path")
  testImplementation("io.rest-assured:xml-path")
  testImplementation("io.rest-assured:spring-mock-mvc")
  testImplementation("io.swagger.parser.v3:swagger-parser-v3:2.1.32")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

kotlin {
  compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
}

tasks {
  val copyAgentJar by registering(Copy::class) {
    from("${project.layout.buildDirectory.get().asFile}/libs")
    include("applicationinsights-agent*.jar")
    into("${project.layout.buildDirectory.get().asFile}/agent")
    rename("applicationinsights-agent(.+).jar", "agent.jar")
    dependsOn("assemble")
  }
  getByName("jib") { dependsOn += copyAgentJar }
  getByName("jibBuildTar") { dependsOn += copyAgentJar }
  getByName("jibDockerBuild") { dependsOn += copyAgentJar }
}

jib {
  container {
    creationTime.set("USE_CURRENT_TIMESTAMP")
    jvmFlags = mutableListOf("-Duser.timezone=Europe/London")
    mainClass = "uk.gov.justice.hmpps.probationsearch.OffenderSearchApplicationKt"
    user = "2000:2000"
  }
  from {
    image = "eclipse-temurin:21-jre-alpine"
  }
  extraDirectories {
    paths {
      path {
        setFrom("${project.layout.buildDirectory.get().asFile}")
        includes.add("agent/agent.jar")
      }
      path {
        setFrom("${project.rootDir}")
        includes.add("applicationinsights*.json")
        into = "/agent"
      }
    }
  }
}

// Disable ktlint in favour of IntelliJ formatting
configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
  filter {
    exclude("**/*")
  }
}
