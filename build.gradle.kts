plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "0.4.8"
  kotlin("plugin.spring") version "1.3.72"
}

extra["spring-security.version"] = "5.3.2.RELEASE" // Updated since spring-boot-starter-oauth2-resource-server-2.2.7.RELEASE only pulls in 5.2.4.RELEASE (still affected by CVE-2018-1258 though)

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
  implementation("io.jsonwebtoken:jjwt:0.9.1")

  implementation("javax.transaction:javax.transaction-api:1.3")
  implementation("javax.xml.bind:jaxb-api:2.3.1")
  implementation("com.sun.xml.bind:jaxb-impl:2.3.3")
  implementation("com.sun.xml.bind:jaxb-core:2.3.0.1")
  implementation("com.google.code.gson:gson:2.8.6")
  implementation("org.apache.commons:commons-lang3:3.11")
  implementation("org.apache.commons:commons-text:1.9")

  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.11.2")

  implementation("org.elasticsearch:elasticsearch:7.4.1")
  implementation("org.elasticsearch.client:elasticsearch-rest-high-level-client:7.4.1")
  implementation("org.elasticsearch.client:elasticsearch-rest-client:7.4.1")

  implementation("com.amazonaws:aws-java-sdk-core:1.11.839")

  implementation("io.springfox:springfox-swagger2:2.9.2")
  implementation("io.springfox:springfox-swagger-ui:2.9.2")
  implementation("io.springfox:springfox-bean-validators:2.9.2")

  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.1")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:2.18.1")
  testImplementation("com.nhaarman:mockito-kotlin-kt1.1:1.6.0")
  testImplementation("org.testcontainers:localstack:1.14.3")
  testImplementation("org.awaitility:awaitility-kotlin:4.0.3")

  testImplementation("io.rest-assured:rest-assured:3.3.0")
  testImplementation("io.rest-assured:spring-mock-mvc:3.3.0")
}
