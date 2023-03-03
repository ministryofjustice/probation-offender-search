package uk.gov.justice.hmpps.offendersearch.cvlsearch

import io.restassured.RestAssured
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.MediaType
import uk.gov.justice.hmpps.offendersearch.controllers.LocalstackIntegrationBase
import uk.gov.justice.hmpps.offendersearch.util.ElasticsearchHelper

internal class CvlSearchIntegrationTest : LocalstackIntegrationBase() {

  @BeforeEach
  fun setUp() {
    ElasticsearchHelper(esClient).loadData()
  }

  private fun cvlSearchRole() = jwtAuthenticationHelper.createJwt("ROLE_CVL_SEARCH")

  @ParameterizedTest
  @MethodSource("teamCodeResults")
  fun `cvl search by team code`(teamCodes: List<String>, query: String, crns: List<String>) {
    val request =
      LicenceCaseloadRequest(teamCodes, query, listOf(SortBy("name.forename", "desc"), SortBy("name.surname")))
    val results = RestAssured.given()
      .auth()
      .oauth2(cvlSearchRole())
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body(request)
      .post("licence-caseload/by-team")
      .then()
      .statusCode(200)
      .extract()
      .body()
      .jsonPath().getList("content", LicenceCaseloadPerson::class.java)

    assertThat(results).hasSize(crns.size)
    assertThat(results.map { it.identifiers.crn }).isEqualTo(crns)
  }

  @ParameterizedTest
  @MethodSource("sortResults")
  fun `cvl search and sort`(sorting: List<SortBy>, crns: List<String>) {
    val request = LicenceCaseloadRequest(teamCodes = listOf("N00UAT", "N02UAT"), sortBy = sorting)
    val results = RestAssured.given()
      .auth()
      .oauth2(cvlSearchRole())
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body(request)
      .post("licence-caseload/by-team")
      .then()
      .statusCode(200)
      .extract()
      .body()
      .jsonPath().getList("content", LicenceCaseloadPerson::class.java)

    assertThat(results).hasSize(crns.size)
    assertThat(results.map { it.identifiers.crn }).isEqualTo(crns)
  }

  @Test
  fun `cvl search returns 400 for invalid requests and lists all errors`() {
    val request = LicenceCaseloadRequest(
      teamCodes = listOf(),
      sortBy = listOf(SortBy("crn", "ascending"), SortBy("name"), SortBy("staff.name", "DESCENDING"))
    )
    val res = RestAssured.given()
      .auth()
      .oauth2(cvlSearchRole())
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body(request)
      .post("licence-caseload/by-team")
      .then()
      .statusCode(400)
      .extract()
      .body()
      .`as`(ErrorsResponse::class.java)

    println(res)

    assertThat(res.fieldErrors).containsAll(
      listOf(
        FieldError("teamCodes", "must not be empty"),
        FieldError("sortBy[0].field", FIELD_MESSAGE),
        FieldError("sortBy[0].direction", DIRECTION_MESSAGE),
        FieldError("sortBy[1].field", FIELD_MESSAGE),
        FieldError("sortBy[2].field", FIELD_MESSAGE),
        FieldError("sortBy[2].direction", DIRECTION_MESSAGE),
      )
    )
  }

  companion object {
    @JvmStatic
    fun teamCodeResults(): List<Arguments> = listOf(
      Arguments.of(listOf("N02UAT"), "", listOf("X00001")),
      Arguments.of(listOf("N00UAT"), "", listOf("X00002", "X00010")),
      Arguments.of(listOf("N00UAT", "N02UAT"), "", listOf("X00001", "X00002", "X00010")),
      Arguments.of(listOf("N00UAT", "N02UAT"), "smith", listOf("X00001", "X00002")),
      Arguments.of(listOf("N00UAT", "N02UAT"), "unallocated", listOf("X00001")),
      Arguments.of(listOf("N02UAT"), "John", listOf("X00001")),
      Arguments.of(listOf("N00UAT"), "Jane", listOf("X00002")),
      Arguments.of(listOf("N00UAT", "N02UAT"), "john", listOf("X00001")),
      Arguments.of(listOf("N00UAT", "N02UAT"), "jane", listOf("X00002")),
      Arguments.of(listOf("N00UAT", "N02UAT"), "X00001", listOf("X00001")),
      Arguments.of(listOf("N00UAT", "N02UAT"), "X00002", listOf("X00002")),
      Arguments.of(listOf("N00UAT", "N02UAT"), "j", listOf("X00001", "X00002", "X00010")),
      Arguments.of(listOf("N00UAT", "N02UAT"), "X0000", listOf("X00001", "X00002")),
      Arguments.of(listOf("N00UAT", "N02UAT"), "0000", listOf("X00001", "X00002")),
      Arguments.of(listOf("N00UAT", "N02UAT"), "ohn", listOf("X00001")),
      Arguments.of(listOf("N00UAT", "N02UAT"), "alloc", listOf("X00001")),
      Arguments.of(listOf("N00UAT", "N02UAT"), "jupiter", listOf<String>()),
    )

    @JvmStatic
    fun sortResults(): List<Arguments> = listOf(
      Arguments.of(listOf(SortBy("identifiers.crn")), listOf("X00001", "X00002", "X00010")),
      Arguments.of(listOf(SortBy("name.forename"), SortBy("name.surname")), listOf("X00010", "X00002", "X00001")),
      Arguments.of(listOf(SortBy("name.surname"), SortBy("name.forename")), listOf("X00010", "X00002", "X00001")),
      Arguments.of(
        listOf(SortBy("name.surname", "desc"), SortBy("name.forename", "desc")),
        listOf("X00001", "X00002", "X00010")
      ),
      Arguments.of(listOf(SortBy("manager.name.forename")), listOf("X00002", "X00001", "X00010")),
      Arguments.of(
        listOf(SortBy("manager.name.surname", "desc"), SortBy("manager.name.forename", "desc")),
        listOf("X00001", "X00002", "X00010")
      ),
    )
  }
}
