package uk.gov.justice.hmpps.offendersearch.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import io.restassured.RestAssured
import io.restassured.RestAssured.given
import io.restassured.config.ObjectMapperConfig
import io.restassured.config.RestAssuredConfig
import org.assertj.core.api.Assertions.assertThat
import org.elasticsearch.client.RestHighLevelClient
import org.hamcrest.CoreMatchers
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestContext
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.support.AbstractTestExecutionListener
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener
import uk.gov.justice.hmpps.offendersearch.dto.OffenderDetail
import uk.gov.justice.hmpps.offendersearch.util.JwtAuthenticationHelper
import uk.gov.justice.hmpps.offendersearch.util.LocalStackHelper
import java.lang.reflect.Type
import java.util.Objects

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["test", "localstack"])
@RunWith(SpringJUnit4ClassRunner::class)
@TestExecutionListeners(listeners = [DependencyInjectionTestExecutionListener::class, OffenderSearchAPIIntegrationTest::class])
@ContextConfiguration
internal class OffenderSearchAPIIntegrationTest : AbstractTestExecutionListener() {
  @Autowired
  private lateinit var jwtAuthenticationHelper: JwtAuthenticationHelper

  override fun beforeTestClass(testContext: TestContext) {
    val objectMapper = testContext.applicationContext.getBean(ObjectMapper::class.java)
    val esClient = testContext.applicationContext.getBean(RestHighLevelClient::class.java)
    LocalStackHelper(esClient).loadData()
    RestAssured.port = Objects.requireNonNull(testContext.applicationContext.environment.getProperty("local.server.port"))!!.toInt()
    RestAssured.config = RestAssuredConfig.config().objectMapperConfig(
      ObjectMapperConfig().jackson2ObjectMapperFactory { _: Type?, _: String? -> objectMapper }
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
  fun `not allowed to do a search without COMMUNITY role`() {
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
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("{\"surname\":\"smith\"}")
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
  fun `can POST or GET a search request`() {
    assertThat(
      given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("{\"surname\": \"smith\"}")
        .post("/search")
        .then()
        .statusCode(200)
        .extract()
        .body()
        .`as`(Array<OffenderDetail>::class.java)
    ).hasSize(2)

    assertThat(
      given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("{\"surname\": \"smith\"}")
        .get("/search")
        .then()
        .statusCode(200)
        .extract()
        .body()
        .`as`(Array<OffenderDetail>::class.java)
    ).hasSize(2)
  }

  @Test
  fun shouldFilterOutSoftDeletedRecords() {
    val results = given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
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
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
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
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
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
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
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
  fun pncNumberShortFormatSearch() {
    val results = given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
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
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
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
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
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
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
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
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
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
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
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
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
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
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
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
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("{}")
      .`when`()["/search"]
      .then()
      .statusCode(400)
      .body("developerMessage", CoreMatchers.containsString("Invalid search  - please provide at least 1 search parameter"))
  }

  @Test
  fun noResults() {
    val results = given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("{\"surname\":\"potter\"}")
      .`when`()["/search"]
      .then()
      .statusCode(200)
      .extract()
      .`as`(Array<OffenderDetail>::class.java)
    assertThat(results).hasSize(0)
  }
}
