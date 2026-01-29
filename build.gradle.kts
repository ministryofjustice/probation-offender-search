import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "10.0.1"
  kotlin("plugin.spring") version "2.3.0"
  id("com.google.cloud.tools.jib") version "3.5.2"
}

dependencies {
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-security-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-reactor-netty")
  implementation("org.springframework.data:spring-data-jpa")
  implementation("io.jsonwebtoken:jjwt-impl:0.13.0")
  implementation("io.jsonwebtoken:jjwt-jackson:0.13.0")
  implementation("org.opensearch.client:spring-data-opensearch-starter:3.0.0")
  implementation("org.opensearch.client:opensearch-java:3.5.0")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.1")
  implementation("io.sentry:sentry-spring-boot-4:8.31.0")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:6.0.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.24.0")
  implementation("io.opentelemetry:opentelemetry-extension-kotlin")
  implementation("io.flipt:flipt-client-java:1.2.1") {
    exclude("org.apache.httpcomponents", "httpclient")
  }

  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.junit.vintage:junit-vintage-engine")
  testImplementation("org.wiremock:wiremock-standalone:3.13.2")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:5.1.0")
  testImplementation("org.awaitility:awaitility-kotlin")
  testImplementation("io.rest-assured:json-path:6.0.0")
  testImplementation("io.rest-assured:xml-path:6.0.0")
  testImplementation("io.rest-assured:spring-mock-mvc:6.0.0")
  testImplementation("io.swagger.parser.v3:swagger-parser-v3:2.1.37")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

kotlin {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_25)
    freeCompilerArgs.set(listOf("-Xannotation-default-target=param-property"))
  }
}

tasks {
  withType<Test>().configureEach {
    jvmArgs("-Dspring.test.context.cache.pause=never")
  }
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
    image = "eclipse-temurin:25-jre-alpine"
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
