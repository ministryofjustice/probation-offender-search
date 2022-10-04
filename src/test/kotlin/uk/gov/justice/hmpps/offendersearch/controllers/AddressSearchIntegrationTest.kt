package uk.gov.justice.hmpps.offendersearch.controllers

import io.restassured.RestAssured
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.MediaType
import uk.gov.justice.hmpps.offendersearch.addresssearch.AddressSearchResponses
import uk.gov.justice.hmpps.offendersearch.util.LocalStackHelper

internal class AddressSearchIntegrationTest : LocalstackIntegrationBase() {

  @BeforeEach
  fun setUp() {
    LocalStackHelper(esClient).loadData()
  }

  @ParameterizedTest
  @MethodSource("postcodeResults")
  fun `postcode search test`(postCode: String, noOfResults: Int) {
    val results = RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("{\"postcode\": \"$postCode\"}")
      .post("/search/addresses")
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(AddressSearchResponses::class.java)

    assertThat(results.personAddresses).hasSize(noOfResults)
  }

  @ParameterizedTest
  @MethodSource("streetNameResults")
  fun `street name search test`(streetName: String, noOfResults: Int) {
    val results = RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("{\"streetName\": \"$streetName\"}")
      .post("/search/addresses")
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(AddressSearchResponses::class.java)

    assertThat(results.personAddresses).hasSize(noOfResults)
  }

  companion object {
    @JvmStatic
    fun postcodeResults(): List<Arguments> = listOf(
      Arguments.of("NE1 2SW",1),
      Arguments.of("ne1 2sw",1),
      Arguments.of("NE1",1),
      Arguments.of("NE12SW",1),
      Arguments.of("2SW",1),
      Arguments.of("GG1 1BB",0),
    )
    @JvmStatic
    fun streetNameResults(): List<Arguments> = listOf(
      Arguments.of("church street",1),
      Arguments.of("Church St",1),
      Arguments.of("Church",1),
      Arguments.of("no street",1),
      Arguments.of("cathedral view",0),
    )
  }


}
