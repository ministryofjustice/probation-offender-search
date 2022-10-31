package uk.gov.justice.hmpps.offendersearch.config

import io.restassured.RestAssured
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.hmpps.offendersearch.controllers.LocalstackIntegrationBase
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SwaggerDocsTest : LocalstackIntegrationBase() {

  @Test
  fun `swagger docs are available`() {
    RestAssured.given()
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .get("/swagger-ui/index.html")
      .then()
      .statusCode(200)
  }

  @Test
  fun `the swagger json is valid`() {
    val response = RestAssured.given()
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .get("/v3/api-docs")
      .then()
      .statusCode(200)
      .extract()
      .body().asString()

    assertThatJson(response).node("messages").isAbsent()
  }

  @Test
  fun `the swagger json contains the version number`() {
    val response = RestAssured.given()
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .get("/v3/api-docs")
      .then()
      .statusCode(200)
      .extract()
      .body().asString()

    assertThatJson(response).node("info.version").isEqualTo(DateTimeFormatter.ISO_DATE.format(LocalDate.now()))
  }
}
