package uk.gov.justice.hmpps.offendersearch.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import io.restassured.RestAssured
import io.restassured.RestAssured.given
import io.restassured.config.ObjectMapperConfig
import io.restassured.config.RestAssuredConfig
import org.elasticsearch.client.RestHighLevelClient
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestContext
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.support.AbstractTestExecutionListener
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener
import uk.gov.justice.hmpps.offendersearch.dto.MatchRequest
import uk.gov.justice.hmpps.offendersearch.util.JwtAuthenticationHelper
import uk.gov.justice.hmpps.offendersearch.util.LocalStackHelper
import java.lang.reflect.Type
import java.time.LocalDate
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test,localstack")
@ExtendWith(SpringExtension::class)
@TestExecutionListeners(listeners = [DependencyInjectionTestExecutionListener::class, OffenderMatchControllerAPIIntegrationTest::class])
@ContextConfiguration
internal class OffenderMatchControllerAPIIntegrationTest : AbstractTestExecutionListener() {
  @Autowired
  private lateinit var jwtAuthenticationHelper: JwtAuthenticationHelper

  override fun beforeTestClass(testContext: TestContext) {
    val objectMapper = testContext.applicationContext.getBean(ObjectMapper::class.java)
    val esClient = testContext.applicationContext.getBean(RestHighLevelClient::class.java)
    LocalStackHelper(esClient).loadData()
    RestAssured.port = Objects.requireNonNull(testContext.applicationContext.environment.getProperty("local.server.port"))!!.toInt()
    RestAssured.config = RestAssuredConfig.config().objectMapperConfig(
        ObjectMapperConfig().jackson2ObjectMapperFactory { _: Type?, _: String? -> objectMapper })
  }

  @Test
  internal fun `access allowed with ROLE_COMMUNITY`() {
    given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("{\"surname\": \"Smith\"}")
        .When()["/match"]
        .then()
        .statusCode(200)
  }

  @Test
  internal fun `without ROLE_COMMUNITY access is denied`() {
    given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_BINGO"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("{\"surname\": \"Smith\"}")
        .When()["/match"]
        .then()
        .statusCode(403)
  }

  @Test
  internal fun `should match when a single offender has all matching attributes`() {
    given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body(MatchRequest(surname = "gramsci", firstName = "anne", dateOfBirth = LocalDate.of(1988, 1, 6)))
        .When()["/match"]
        .then()
        .statusCode(200)
        .body("matches.findall.size()", equalTo(1))
        .body("matches[0].offender.otherIds.crn", equalTo("X00007"))
  }

}

