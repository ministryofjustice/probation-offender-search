plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "4.1.7"
  kotlin("plugin.spring") version "1.6.21"
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

  implementation("javax.transaction:javax.transaction-api")
  implementation("javax.xml.bind:jaxb-api")
  implementation("com.sun.xml.bind:jaxb-impl:3.0.2")
  implementation("com.sun.xml.bind:jaxb-core:3.0.2")
  implementation("com.google.code.gson:gson")
  implementation("org.apache.commons:commons-lang3:3.12.0")
  implementation("org.apache.commons:commons-text:1.9")

  implementation("org.elasticsearch:elasticsearch")
  implementation("org.elasticsearch.client:elasticsearch-rest-high-level-client")
  implementation("org.elasticsearch.client:elasticsearch-rest-client")

  implementation("com.amazonaws:aws-java-sdk-core:1.12.214")

  implementation("io.springfox:springfox-boot-starter:3.0.0")

  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.2")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:2.35.0")
  testImplementation("org.mockito:mockito-inline")
  testImplementation("org.testcontainers:localstack:1.17.2")
  testImplementation("org.awaitility:awaitility-kotlin")
  testImplementation("io.rest-assured:json-path")
  testImplementation("io.rest-assured:xml-path")
  testImplementation("io.rest-assured:spring-mock-mvc")
  testImplementation("io.swagger.parser.v3:swagger-parser-v3:2.0.32")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(16))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "16"
    }
  }

  test {
    maxHeapSize = "256m"
  }
}
