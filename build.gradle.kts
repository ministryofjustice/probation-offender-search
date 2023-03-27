plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "5.1.2"
  kotlin("plugin.spring") version "1.8.10"
  id("com.google.cloud.tools.jib") version "3.3.1"
}

configurations {
  implementation { exclude(group = "tomcat-jdbc") }
}

dependencyCheck {
  suppressionFiles.add("dependency-check-suppress-es.xml")
}

dependencies {
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.data:spring-data-jpa")

  implementation("io.jsonwebtoken:jjwt-impl:0.11.5")
  implementation("io.jsonwebtoken:jjwt-jackson:0.11.5")

  implementation("org.springframework.data:spring-data-elasticsearch:4.2.12")

  implementation("com.amazonaws:aws-java-sdk-core:1.12.+")

  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.0.4")

  implementation("io.sentry:sentry-spring-boot-starter:6.16.0")

  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("com.github.tomakehurst:wiremock-jre8-standalone:2.35.0")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:2.37.0")
  testImplementation("org.mockito:mockito-inline")
  testImplementation("org.awaitility:awaitility-kotlin")
  testImplementation("io.rest-assured:json-path")
  testImplementation("io.rest-assured:xml-path")
  testImplementation("io.rest-assured:spring-mock-mvc")
  testImplementation("io.swagger.parser.v3:swagger-parser-v3:2.1.13")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks {
  val copyAgentJar by registering(Copy::class) {
    from("${project.buildDir}/libs")
    include("applicationinsights-agent*.jar")
    into("${project.buildDir}/agent")
    rename("applicationinsights-agent(.+).jar", "agent.jar")
    dependsOn("assemble")
  }

  val jib by getting {
    dependsOn += copyAgentJar
  }

  val jibBuildTar by getting {
    dependsOn += copyAgentJar
  }

  val jibDockerBuild by getting {
    dependsOn += copyAgentJar
  }

  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "17"
    }
  }

  test {
    maxHeapSize = "256m"
  }
}

jib {
  container {
    creationTime.set("USE_CURRENT_TIMESTAMP")
    jvmFlags = mutableListOf("-Duser.timezone=Europe/London")
    mainClass = "uk.gov.justice.hmpps.probationsearch.OffenderSearchApplicationKt"
    user = "2000:2000"
  }
  from {
    image = "ghcr.io/ministryofjustice/hmpps-probation-integration-services/eclipse-temurin:17-jre-alpine"
  }
  extraDirectories {
    paths {
      path {
        setFrom("${project.buildDir}")
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
