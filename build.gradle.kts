plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "3.1.4"
  kotlin("plugin.spring") version "1.4.32"
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
  implementation("com.sun.xml.bind:jaxb-impl:3.0.0")
  implementation("com.sun.xml.bind:jaxb-core:3.0.0")
  implementation("com.google.code.gson:gson:2.8.6")
  implementation("org.apache.commons:commons-lang3:3.12.0")
  implementation("org.apache.commons:commons-text:1.9")

  implementation("org.elasticsearch:elasticsearch:7.12.0")
  implementation("org.elasticsearch.client:elasticsearch-rest-high-level-client:7.12.0")
  implementation("org.elasticsearch.client:elasticsearch-rest-client:7.12.0")

  implementation("com.amazonaws:aws-java-sdk-core:1.11.989")

  implementation("io.springfox:springfox-boot-starter:3.0.0")

  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.2")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:2.25.0")
  testImplementation("com.nhaarman:mockito-kotlin-kt1.1:1.6.0")
  testImplementation("org.mockito:mockito-inline:3.8.0")
  testImplementation("org.testcontainers:localstack:1.15.2")
  testImplementation("org.awaitility:awaitility-kotlin:4.0.3")

  testImplementation("io.rest-assured:json-path:4.3.3")
  testImplementation("io.rest-assured:xml-path:4.3.3")
  testImplementation("io.rest-assured:spring-mock-mvc:4.3.3")
}

tasks {
  test {
    maxHeapSize = "512m"
  }
}
