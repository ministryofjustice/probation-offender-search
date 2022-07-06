plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "4.3.2"
  kotlin("plugin.spring") version "1.7.0"
  id("com.google.cloud.tools.jib") version "3.2.1"
}

configurations {
  implementation { exclude(group = "tomcat-jdbc") }
}

dependencyCheck {
  suppressionFiles.add("dependency-check-suppress-es.xml")
}

ext["elasticsearch.version"] = "7.13.4"
dependencies {
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.data:spring-data-jpa")

  implementation("io.jsonwebtoken:jjwt:0.9.1")

  implementation("com.google.code.gson:gson")
  implementation("org.apache.commons:commons-lang3:3.12.0")
  implementation("org.apache.commons:commons-text:1.9")

  implementation("org.elasticsearch:elasticsearch")
  implementation("org.elasticsearch.client:elasticsearch-rest-high-level-client")
  implementation("org.elasticsearch.client:elasticsearch-rest-client")

  implementation("com.amazonaws:aws-java-sdk-core:1.12.251")

  implementation("io.springfox:springfox-boot-starter:3.0.0")

  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.2")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:2.35.0")
  testImplementation("org.mockito:mockito-inline")
  testImplementation("org.testcontainers:localstack:1.17.3")
  testImplementation("org.awaitility:awaitility-kotlin")
  testImplementation("io.rest-assured:json-path")
  testImplementation("io.rest-assured:xml-path")
  testImplementation("io.rest-assured:spring-mock-mvc")
  testImplementation("io.swagger.parser.v3:swagger-parser-v3:2.1.1")
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
    creationTime = "USE_CURRENT_TIMESTAMP"
    jvmFlags = mutableListOf("-Duser.timezone=Europe/London")
    mainClass = "uk.gov.justice.hmpps.offendersearch.OffenderSearchApplicationKt"
    user = "2000:2000"
  }
  from {
    image = "eclipse-temurin:17-jre-alpine"
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
