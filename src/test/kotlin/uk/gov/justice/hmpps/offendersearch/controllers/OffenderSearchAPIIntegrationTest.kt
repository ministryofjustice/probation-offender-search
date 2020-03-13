package uk.gov.justice.hmpps.offendersearch.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import io.restassured.RestAssured
import io.restassured.config.ObjectMapperConfig
import io.restassured.config.RestAssuredConfig
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.elasticsearch.client.RestHighLevelClient
import org.hamcrest.CoreMatchers
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Value
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
import uk.gov.justice.hmpps.offendersearch.util.LocalStackHelper
import java.lang.reflect.Type
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev,localstack")
@RunWith(SpringJUnit4ClassRunner::class)
@TestExecutionListeners(listeners = [DependencyInjectionTestExecutionListener::class, OffenderSearchAPIIntegrationTest::class])
@ContextConfiguration
class OffenderSearchAPIIntegrationTest : AbstractTestExecutionListener() {
  @Value("\${test.token.good}")
  private lateinit var validOauthToken: String

  override fun beforeTestClass(testContext: TestContext) {
    val objectMapper = testContext.applicationContext.getBean(ObjectMapper::class.java)
    val esClient = testContext.applicationContext.getBean(RestHighLevelClient::class.java)
    LocalStackHelper(esClient).loadData()
    RestAssured.port = Objects.requireNonNull(testContext.applicationContext.environment.getProperty("local.server.port"))!!.toInt()
    RestAssured.config = RestAssuredConfig.config().objectMapperConfig(
        ObjectMapperConfig().jackson2ObjectMapperFactory { _: Type?, _: String? -> objectMapper })
  }

  @Test
  fun surnameSearch() {
    val results = RestAssured.given()
        .auth()
        .oauth2(validOauthToken)
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
  fun prisonNumberSearch() {
    val results = RestAssured.given()
        .auth()
        .oauth2(validOauthToken)
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
  fun allParameters() {
    val results = RestAssured.given()
        .auth()
        .oauth2(validOauthToken)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("{\"surname\": \"smith\",\"firstName\": \"John\",\"crn\": \"X00001\",\"croNumber\": \"SF80/655108T\", \"nomsNumber\": \"G8020GG\",\"pncNumber\": \"2018/0123456X\"}\n")
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
    RestAssured.given()
        .auth()
        .oauth2(validOauthToken)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("{}")
        .`when`()["/search"]
        .then()
        .statusCode(400)
        .body("developerMessage", CoreMatchers.containsString("Invalid search  - please provide at least 1 search parameter"))
  }

  @Test
  fun noResults() {
    val results = RestAssured.given()
        .auth()
        .oauth2(validOauthToken)
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