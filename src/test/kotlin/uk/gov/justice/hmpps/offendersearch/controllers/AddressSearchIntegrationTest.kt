package uk.gov.justice.hmpps.offendersearch.controllers

import io.restassured.RestAssured
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.hmpps.offendersearch.util.LocalStackHelper

internal class AddressSearchIntegrationTest : LocalstackIntegrationBase()  {

  @BeforeEach
  fun setUp() {
    LocalStackHelper(esClient).loadData()
  }

  @Test
  fun `OK response with valid request`() {
    RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("{\"postcode\": \"NE1 2SW\"}")
      .post("/search/addresses")
      .then()
      .statusCode(200)
  }
}
