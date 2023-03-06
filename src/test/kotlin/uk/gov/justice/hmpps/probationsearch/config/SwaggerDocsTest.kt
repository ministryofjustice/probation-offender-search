package uk.gov.justice.hmpps.probationsearch.config

import io.restassured.RestAssured
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.hmpps.probationsearch.controllers.ElasticIntegrationBase

class SwaggerDocsTest : ElasticIntegrationBase() {

  @Test
  fun `swagger docs are available`() {
    RestAssured.given()
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .get("/swagger-ui/index.html")
      .then()
      .statusCode(200)
  }
}
