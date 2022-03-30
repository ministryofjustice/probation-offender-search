plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "4.1.1"
  kotlin("plugin.spring") version "1.6.0"
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

  implementation("io.jsonwebtoken:jjwt:0.9.1")

  implementation("javax.transaction:javax.transaction-api:1.3")
  implementation("javax.xml.bind:jaxb-api:2.3.1")
  implementation("com.sun.xml.bind:jaxb-impl:3.0.2")
  implementation("com.sun.xml.bind:jaxb-core:3.0.2")
  implementation("com.google.code.gson:gson:2.8.9")
  implementation("org.apache.commons:commons-lang3:3.12.0")
  implementation("org.apache.commons:commons-text:1.9")

  implementation("org.elasticsearch:elasticsearch:7.13.4")
  implementation("org.elasticsearch.client:elasticsearch-rest-high-level-client:7.13.4")
  implementation("org.elasticsearch.client:elasticsearch-rest-client:7.13.4")

  implementation("com.amazonaws:aws-java-sdk-core:1.12.129")

  implementation("io.springfox:springfox-boot-starter:3.0.0")

  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.2")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:2.28.0")
  testImplementation("org.mockito:mockito-inline:4.4.0")
  testImplementation("org.testcontainers:localstack:1.16.2")
  testImplementation("org.awaitility:awaitility-kotlin:4.1.1")

  testImplementation("io.rest-assured:json-path:4.4.0")
  testImplementation("io.rest-assured:xml-path:4.4.0")
  testImplementation("io.rest-assured:spring-mock-mvc:4.4.0")
  testImplementation("io.swagger.parser.v3:swagger-parser-v3:2.0.20")
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
