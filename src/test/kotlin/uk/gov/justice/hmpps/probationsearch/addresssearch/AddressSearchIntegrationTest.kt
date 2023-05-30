package uk.gov.justice.hmpps.probationsearch.addresssearch

import io.restassured.RestAssured
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.MediaType
import uk.gov.justice.hmpps.probationsearch.controllers.ElasticIntegrationBase
import uk.gov.justice.hmpps.probationsearch.util.PersonSearchHelper

internal class AddressSearchIntegrationTest : ElasticIntegrationBase() {

  @BeforeEach
  fun setUp() {
    PersonSearchHelper(openSearchClient).loadData()
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
  fun `must not match on street name only`(streetName: String, noOfResults: Int) {
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

  @Test
  fun `matching on number alone gives no result`() {
    // given data exists with a matching address number and street
    val existing = RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("{\"addressNumber\": \"29\", \"streetName\": \"Church Street\", \"postcode\": \"NE1 2SW\"}")
      .post("/search/addresses")
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(AddressSearchResponses::class.java)

    assertThat(existing.personAddresses).hasSize(2)

    // searching on number alone gives no results
    val results = RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("{\"addressNumber\": \"29\"}")
      .post("/search/addresses")
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(AddressSearchResponses::class.java)

    assertThat(results.personAddresses).hasSize(0)
  }

  @ParameterizedTest
  @MethodSource("multipartResults")
  fun `must match on all provided parts`(body: String, noOfResults: Int) {
    val existing = RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body(body)
      .post("/search/addresses")
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(AddressSearchResponses::class.java)

    assertThat(existing.personAddresses.size).isEqualTo(noOfResults)
  }

  @ParameterizedTest
  @MethodSource("townResults")
  fun `must not match on town only`(body: String, noOfResults: Int) {
    val existing = RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body(body)
      .post("/search/addresses")
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(AddressSearchResponses::class.java)

    assertThat(existing.personAddresses.size).isEqualTo(noOfResults)
  }

  companion object {
    @JvmStatic
    fun postcodeResults(): List<Arguments> = listOf(
      Arguments.of("NE1 2SW", 2),
      Arguments.of("ne1 2sw", 2),
      Arguments.of("NE12SW", 2),
      Arguments.of("NE1", 0),
      Arguments.of("2SW", 0),
      Arguments.of("GG1 1BB", 0),
    )

    @JvmStatic
    fun streetNameResults(): List<Arguments> = listOf(
      Arguments.of("church street", 0),
      Arguments.of("Church St", 0),
      Arguments.of("Church", 0),
      Arguments.of("no street", 0),
      Arguments.of("cathedral view", 0),
    )

    @JvmStatic
    fun multipartResults(): List<Arguments> = listOf(
      Arguments.of("{\"postcode\": \"NE1  2SW\", \"streetName\": \"Church Street\"}", 2),
      Arguments.of("{\"postcode\": \"NE1 2SW\", \"streetName\": \"Church St\"}", 2),
      Arguments.of("{\"postcode\": \"NE1 2SW\", \"streetName\": \"Church Lane\"}", 2),
      Arguments.of("{\"postcode\": \"NE2 2SW\", \"streetName\": \"Church Street\"}", 1),
      Arguments.of("{\"postcode\": \"NE2 2SW\", \"streetName\": \"Church Lane\"}", 1),
      Arguments.of("{\"postcode\": \"NE2 2SW\", \"streetName\": \"Church Street Lane\"}", 1),
    )

    @JvmStatic
    fun townResults(): List<Arguments> = listOf(
      Arguments.of("{\"town\": \"Newcastle upon Tyne\"}", 0),
      Arguments.of("{\"town\": \"newcastle upon tyne\"}", 0),
      Arguments.of("{\"town\": \"Newcastle under Lyme\"}", 0),
      Arguments.of("{\"town\": \"Newcastle\"}", 0),
    )
  }
}
