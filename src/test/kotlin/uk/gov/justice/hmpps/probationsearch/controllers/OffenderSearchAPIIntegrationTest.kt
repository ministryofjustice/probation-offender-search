package uk.gov.justice.hmpps.probationsearch.controllers

import io.restassured.RestAssured
import io.restassured.RestAssured.given
import io.restassured.config.ObjectMapperConfig
import io.restassured.config.RestAssuredConfig
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.opensearch.client.RestHighLevelClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.hmpps.probationsearch.OpenSearchIntegrationTest
import uk.gov.justice.hmpps.probationsearch.dto.OffenderDetail
import uk.gov.justice.hmpps.probationsearch.services.FeatureFlags
import uk.gov.justice.hmpps.probationsearch.util.JwtAuthenticationHelper
import uk.gov.justice.hmpps.probationsearch.util.PersonSearchHelper
import java.lang.reflect.Type

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@MockitoBean(types = [FeatureFlags::class])
@ActiveProfiles(profiles = ["test"])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OffenderSearchAPIIntegrationTest : OpenSearchIntegrationTest() {
  @Autowired
  private lateinit var jwtAuthenticationHelper: JwtAuthenticationHelper

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Autowired
  private lateinit var esClient: RestHighLevelClient

  @LocalServerPort
  private var port: Int = 0

  @BeforeAll
  fun beforeAll() {
    PersonSearchHelper(esClient).loadData()
    RestAssured.port = port
    RestAssured.config = RestAssuredConfig.config().objectMapperConfig(
      ObjectMapperConfig().jackson3ObjectMapperFactory { _: Type?, _: String? -> objectMapper },
    )
  }

  @Test
  fun `can access info without valid token`() {
    given()
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .`when`()["/info"]
      .then()
      .statusCode(200)
  }

  @Test
  fun `can access ping without valid token`() {
    given()
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .`when`()["/health/ping"]
      .then()
      .statusCode(200)
  }

  @Test
  fun `not allowed to do a search without appropriate role`() {
    given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_BINGO"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("{\"surname\":\"smith\"}")
      .`when`()["/search"]
      .then()
      .statusCode(403)
  }

  @Test
  fun surnameSearch() {
    val results = given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_PROBATION__SEARCH_PERSON"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("{\"surname\":\"smith\", \"includeAliases\": false}")
      .`when`()["/search"]
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(Array<OffenderDetail>::class.java)
    assertThat(results).hasSize(2)
    assertThat(results).extracting("firstName").containsExactlyInAnyOrder("John", "Jane")
  }

  @Test
  fun surnameSearchWithAliases() {
    val results = given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_PROBATION__SEARCH_PERSON"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("{\"surname\":\"smith\", \"includeAliases\": true}")
      .`when`()["/search"]
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(Array<OffenderDetail>::class.java)
    assertThat(results).hasSize(4)
    assertThat(results).extracting("firstName").containsExactlyInAnyOrder("John", "Jane", "James", "Antonio")
  }

  @Test
  fun `can POST or GET a search request`() {
    assertThat(
      given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_PROBATION__SEARCH_PERSON"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("{\"surname\": \"smith\"}")
        .post("/search")
        .then()
        .statusCode(200)
        .extract()
        .body()
        .`as`(Array<OffenderDetail>::class.java),
    ).hasSize(2)

    assertThat(
      given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_PROBATION__SEARCH_PERSON"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("{\"surname\": \"smith\"}")
        .get("/search")
        .then()
        .statusCode(200)
        .extract()
        .body()
        .`as`(Array<OffenderDetail>::class.java),
    ).hasSize(2)
  }

  @Test
  fun shouldFilterOutSoftDeletedRecords() {
    val results = given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_PROBATION__SEARCH_PERSON"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("{\"surname\":\"Jones\"}")
      .`when`()["/search"]
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(Array<OffenderDetail>::class.java)
    assertThat(results).hasSize(0)
  }

  @Test
  fun nomsNumberSearch() {
    val results = given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_PROBATION__SEARCH_PERSON"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("{\"nomsNumber\":\"G8020GG\"}")
      .`when`()["/search"]
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(Array<OffenderDetail>::class.java)
    assertThat(results).hasSize(1)
    assertThat(results).extracting("firstName").containsExactly("John")
  }

  @Test
  fun prisonNumberSearch() {
    val results = given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_PROBATION__SEARCH_PERSON"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("{\"nomsNumber\":\"G8020GG\"}")
      .`when`()["/search"]
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(Array<OffenderDetail>::class.java)
    assertThat(results).hasSize(1)
    assertThat(results).extracting("firstName").containsExactly("John")
  }

  @Test
  fun dateOfBirthSearch() {
    val results = given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_PROBATION__SEARCH_PERSON"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("{\"dateOfBirth\": \"1978-01-06\"}")
      .`when`()["/search"]
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(Array<OffenderDetail>::class.java)
    assertThat(results).hasSize(1)
    assertThat(results).extracting("firstName").containsExactly("John")
  }

  @Test
  fun dateOfBirthSearchWithAliases() {
    val results = given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_PROBATION__SEARCH_PERSON"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("{\"dateOfBirth\": \"1978-01-06\", \"includeAliases\": true}")
      .`when`()["/search"]
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(Array<OffenderDetail>::class.java)
    assertThat(results).hasSize(3)
    assertThat(results).extracting("firstName").containsExactlyInAnyOrder("John", "James", "Antonio")
  }

  @Test
  fun pncNumberShortFormatSearch() {
    val results = given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_PROBATION__SEARCH_PERSON"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("{\"pncNumber\":\"18/123456X\"}")
      .`when`()["/search"]
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(Array<OffenderDetail>::class.java)
    assertThat(results).hasSize(1)
    assertThat(results).extracting("firstName").containsExactly("John")
  }

  @Test
  fun pncNumberLongFormatSearch() {
    val results = given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_PROBATION__SEARCH_PERSON"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("{\"pncNumber\":\"2018/0123456X\"}")
      .`when`()["/search"]
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(Array<OffenderDetail>::class.java)
    assertThat(results).hasSize(1)
    assertThat(results).extracting("firstName").containsExactly("John")
  }

  @Test
  fun croNumberLongFormatSearch() {
    val results = given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_PROBATION__SEARCH_PERSON"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("{\"croNumber\":\"SF80/777108T\"}")
      .`when`()["/search"]
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(Array<OffenderDetail>::class.java)
    assertThat(results).hasSize(1)
    assertThat(results).extracting("firstName").containsExactly("Jane")
  }

  @Test
  fun croNumberLongFormatSearchAndSurname() {
    val results = given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_PROBATION__SEARCH_PERSON"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("{\"croNumber\":\"SF80/777108T\",\"surname\":\"SMITH\"}")
      .`when`()["/search"]
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(Array<OffenderDetail>::class.java)
    assertThat(results).hasSize(1)
    assertThat(results).extracting("firstName").containsExactly("Jane")
  }

  @Test
  fun pncNumberLongFormatSearchAndSurname() {
    val results = given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_PROBATION__SEARCH_PERSON"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("{\"pncNumber\":\"2018/0123456X\", \"surname\":\"SMITH\"}")
      .`when`()["/search"]
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(Array<OffenderDetail>::class.java)
    assertThat(results).hasSize(1)
    assertThat(results).extracting("firstName").containsExactly("John")
  }

  @Test
  fun pncNumberLongFormatSearchAndWrongSurname() {
    val results = given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_PROBATION__SEARCH_PERSON"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("{\"pncNumber\":\"2018/0123456X\", \"surname\":\"Denton\"}")
      .`when`()["/search"]
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(Array<OffenderDetail>::class.java)
    assertThat(results).hasSize(0)
  }

  @Test
  fun allParameters() {
    val results = given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_PROBATION__SEARCH_PERSON"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("{\"surname\": \"smith\",\"firstName\": \"John\",\"crn\": \"X00001\",\"croNumber\": \"SF80/655108T\", \"nomsNumber\": \"G8020GG\",\"pncNumber\": \"2018/0123456X\", \"dateOfBirth\": \"1978-01-06\"}\n")
      .`when`()["/search"]
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(Array<OffenderDetail>::class.java)
    assertThat(results).hasSize(1)
    assertThat(results).extracting("firstName").containsExactly("John")
  }

  @Test
  fun blanksShouldBeIgnored() {
    val results = given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_PROBATION__SEARCH_PERSON"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("{\"surname\": \" \",\"firstName\": \" \",\"crn\": \" \",\"croNumber\": \" \", \"nomsNumber\": \" \",\"pncNumber\": \" \", \"dateOfBirth\": \"1978-01-06\"}\n")
      .`when`()["/search"]
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(Array<OffenderDetail>::class.java)
    assertThat(results).hasSize(1)
    assertThat(results).extracting("firstName").containsExactly("John")
  }

  @Test
  fun noSearchParameters_badRequest() {
    given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_PROBATION__SEARCH_PERSON"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("{}")
      .`when`()["/search"]
      .then()
      .statusCode(400)
      .body(
        "developerMessage",
        CoreMatchers.containsString("Invalid search  - please provide at least 1 search parameter"),
      )
  }

  @Test
  fun noResults() {
    val results = given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_PROBATION__SEARCH_PERSON"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("{\"surname\":\"potter\"}")
      .`when`()["/search"]
      .then()
      .statusCode(200)
      .extract()
      .`as`(Array<OffenderDetail>::class.java)
    assertThat(results).hasSize(0)
  }

  @Test
  fun previousCrnSearch() {
    val results = given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_PROBATION__SEARCH_PERSON"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("{\"crn\":\"X10001\"}")
      .`when`()["/search"]
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(Array<OffenderDetail>::class.java)
    assertThat(results).hasSize(1)
    assertThat(results).extracting("firstName").contains("John")
    assertThat(results).extracting("surname").contains("Smith")
  }

  @Test
  fun findsOnlyCompleteMatchesWhenUsingAlias() {
    val results = given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_PROBATION__SEARCH_PERSON"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("{\"surname\": \"smith\",\"firstName\": \"John\", \"dateOfBirth\": \"1978-01-06\", \"includeAliases\": true}\n")
      .`when`()["/search"]
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(Array<OffenderDetail>::class.java)
    assertThat(results).hasSize(2)
    assertThat(results).extracting("firstName").containsExactly("John", "James")
  }

  @Test
  fun `paginated search returns count and results`() {
    given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_PROBATION__SEARCH_PERSON"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .queryParam("page", 1)
      .queryParam("size", 2)
      .body("{\"surname\":\"smith\", \"includeAliases\": true}")
      .`when`()["/search/people"]
      .then()
      .statusCode(200)
      .body("content.size()", Matchers.equalTo(2))
      .body("totalElements", Matchers.equalTo(4))
      .body("totalPages", Matchers.equalTo(2))
      .body("pageable.offset", Matchers.equalTo(2))
  }
}
