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
import org.springframework.beans.factory.annotation.Qualifier
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
import uk.gov.justice.hmpps.offendersearch.dto.OffenderDetail
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

  @Autowired
  @Qualifier("elasticSearchClient")
  private lateinit var esClient: RestHighLevelClient

  @Autowired
  private lateinit var objectMapper: ObjectMapper

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
        .post("/match")
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
        .post("/match")
        .then()
        .statusCode(403)
  }

  @Test
  internal fun `should match when a single offender has all matching attributes`() {
    loadOffenders(
        OffenderIdentification(
            surname = "gramsci",
            firstName = "anne",
            dateOfBirth = LocalDate.of(1988, 1, 6),
            crn = "X00007",
            nomsNumber = "G5555TT",
            croNumber = "SF80/655108T",
            pncNumber = "2018/x"
        ),
        OffenderIdentification(
            surname = "smith",
            firstName = "john",
            dateOfBirth = LocalDate.of(1921, 1, 6),
            crn = "X00001"
        )
    )

    given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body(MatchRequest(
            surname = "gramsci",
            firstName = "anne",
            dateOfBirth = LocalDate.of(1988, 1, 6),
            nomsNumber = "G5555TT",
            croNumber = "SF80/655108T",
            pncNumber = "2018/0123456"
        ))
        .post("/match")
        .then()
        .statusCode(200)
        .body("matches.findall.size()", equalTo(1))
        .body("matches[0].offender.otherIds.crn", equalTo("X00007"))
  }

  private fun loadOffenders(vararg offenders: OffenderIdentification) {
    val template = "/elasticsearchdata/offender-template.json".readResourceAsText()
    val templateOffender = objectMapper.readValue(template, OffenderDetail::class.java)

    val offendersToLoad = offenders.map {
      templateOffender.copy(
          surname = it.surname,
          firstName = it.firstName,
          dateOfBirth = it.dateOfBirth,
          otherIds = templateOffender.otherIds?.copy(
              crn = it.crn,
              nomsNumber = it.nomsNumber,
              croNumber = it.croNumber,
              pncNumber = it.pncNumber
          )

      )
    }.map { objectMapper.writeValueAsString(it) }

    LocalStackHelper(esClient).loadData(offendersToLoad)
  }

}

private fun String.readResourceAsText(): String {
  return OffenderDetail::class.java.getResource(this).readText()
}

data class OffenderIdentification(
    val surname: String,
    val firstName: String,
    val dateOfBirth: LocalDate,
    val crn: String,
    val nomsNumber: String? = null,
    val croNumber: String? = null,
    val pncNumber: String? = null
)