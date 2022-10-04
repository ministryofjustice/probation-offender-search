package uk.gov.justice.hmpps.offendersearch.controllers

import io.restassured.RestAssured
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.hmpps.offendersearch.addresssearch.AddressSearchResponses
import uk.gov.justice.hmpps.offendersearch.util.LocalStackHelper

internal class AddressSearchIntegrationTest : LocalstackIntegrationBase() {

  @BeforeEach
  fun setUp() {
    LocalStackHelper(esClient).loadData()
  }

  @Test
  fun `OK response with valid request post code with space`() {
    val results = RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("{\"postcode\": \"NE1 2SW\"}")
      .post("/search/addresses")
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(AddressSearchResponses::class.java)

    assertThat(results.personAddresses).hasSize(1)
  }

  @Test
  fun `OK response with valid request postcode no space`() {
    val results = RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("{\"postcode\": \"NE12SW\"}")
      .post("/search/addresses")
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(AddressSearchResponses::class.java)

    assertThat(results.personAddresses).hasSize(1)
  }

  @Test
  fun `OK response with valid request partial postcode`() {
    var results = RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("{\"postcode\": \"NE1\"}")
      .post("/search/addresses")
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(AddressSearchResponses::class.java)

    assertThat(results.personAddresses).hasSize(1)

    results = RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("{\"postcode\": \"2sw\"}")
      .post("/search/addresses")
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(AddressSearchResponses::class.java)

    assertThat(results.personAddresses).hasSize(1)
  }
}
