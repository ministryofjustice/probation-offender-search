package uk.gov.justice.hmpps.offendersearch.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import io.restassured.RestAssured
import io.restassured.config.ObjectMapperConfig
import io.restassured.config.RestAssuredConfig
import org.elasticsearch.client.RestHighLevelClient
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestContext
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.support.AbstractTestExecutionListener
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener
import uk.gov.justice.hmpps.offendersearch.dto.OffenderAlias
import uk.gov.justice.hmpps.offendersearch.dto.OffenderDetail
import uk.gov.justice.hmpps.offendersearch.dto.ProbationArea
import uk.gov.justice.hmpps.offendersearch.dto.SearchPhraseFilter
import uk.gov.justice.hmpps.offendersearch.util.JwtAuthenticationHelper
import uk.gov.justice.hmpps.offendersearch.util.LocalStackHelper
import java.lang.reflect.Type
import java.time.LocalDate
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["test", "localstack"])
@ExtendWith(SpringExtension::class)
@TestExecutionListeners(listeners = [DependencyInjectionTestExecutionListener::class, OffenderSearchPhraseAPIIntegrationTest::class])
@ContextConfiguration
@DirtiesContext
class OffenderSearchPhraseAPIIntegrationTest : AbstractTestExecutionListener() {
  @Autowired
  internal lateinit var jwtAuthenticationHelper: JwtAuthenticationHelper

  @Autowired
  @Qualifier("elasticSearchClient")
  private lateinit var esClient: RestHighLevelClient

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Value("\${search.supported.mapping.version}")
  private lateinit var mappingVersion: String

  override fun beforeTestClass(testContext: TestContext) {
    val objectMapper = testContext.applicationContext.getBean(ObjectMapper::class.java)
    val esClient = testContext.applicationContext.getBean(RestHighLevelClient::class.java)
    val mappingVersion = testContext.applicationContext.environment.getProperty("search.supported.mapping.version")
    LocalStackHelper(esClient, "v${mappingVersion}").loadData()
    RestAssured.port = Objects.requireNonNull(testContext.applicationContext.environment.getProperty("local.server.port"))!!
        .toInt()
    RestAssured.config = RestAssuredConfig.config().objectMapperConfig(
        ObjectMapperConfig().jackson2ObjectMapperFactory { _: Type?, _: String? -> objectMapper })
  }

  @Test
  internal fun `should match when a single offender`() {
    loadOffenders(
        OffenderReplacement(
            surname = "gramsci",
            firstName = "anne",
            dateOfBirth = LocalDate.of(1988, 1, 6),
            crn = "X00007",
            nomsNumber = "G5555TT",
            croNumber = "SF80/655108T",
            pncNumber = "2018/0123456X"
        )
    )

    RestAssured.given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body(SearchPhraseFilter(
            phrase = "gramsci"
        ))
        .post("/phrase")
        .then()
        .statusCode(200)
        .body("total", equalTo(1))
        .body("probationAreaAggregations", notNullValue())
        .body("suggestions", notNullValue())
        .body("offenders.size()", equalTo(1))
        .body("offenders[0].otherIds.crn", equalTo("X00007"))
  }

  fun loadOffenders(vararg offenders: OffenderReplacement) {
    val template = "/elasticsearchdata/offender-template.json".readResourceAsText()
    val templateOffender = objectMapper.readValue(template, OffenderDetail::class.java)

    val offendersToLoad = offenders.map {
      templateOffender.copy(
          surname = it.surname,
          firstName = it.firstName,
          middleNames = it.middleNames,
          dateOfBirth = it.dateOfBirth,
          softDeleted = it.deleted,
          gender = it.gender,
          otherIds = templateOffender.otherIds?.copy(
              crn = it.crn,
              nomsNumber = it.nomsNumber,
              croNumber = it.croNumber,
              pncNumber = it.pncNumber,
              niNumber = it.niNumber
          ),
          offenderAliases = it.aliases.map { alias ->
            OffenderAlias(
                firstName = alias.firstName,
                surname = alias.surname,
                dateOfBirth = alias.dateOfBirth
            )
          },
          contactDetails = templateOffender.contactDetails?.copy(
              addresses = templateOffender.contactDetails?.addresses?.map { address ->
                if (address.status?.code == "M") address.copy(
                    streetName = it.streetName,
                    town = it.town,
                    county = it.county,
                    postcode = it.postcode
                ) else address
              }
          ),
          offenderManagers = templateOffender.offenderManagers?.map { offenderManager ->
            it.offenderManagers.find { replacement -> replacement.active == offenderManager.active }
                .let { matchingReplacement ->
                  offenderManager.copy(
                      probationArea = ProbationArea(code = matchingReplacement?.code, description = matchingReplacement?.description)
                  )
                }
          }


      )
    }.map { objectMapper.writeValueAsString(it) }

    LocalStackHelper(esClient, "v${mappingVersion}").loadData(offendersToLoad)
  }

}

private fun String.readResourceAsText(): String {
  return OffenderSearchPhraseAPIIntegrationTest::class.java.getResource(this).readText()
}

data class OffenderReplacement(
    val surname: String = "Smith",
    val firstName: String = "John",
    val middleNames: List<String> = listOf(),
    val dateOfBirth: LocalDate = LocalDate.parse("1965-07-19"),
    val crn: String = "X12345",
    val gender: String = "Male",
    val deleted: Boolean = false,
    val aliases: List<AliasReplacement> = listOf(),
    val nomsNumber: String? = null,
    val croNumber: String? = null,
    val pncNumber: String? = null,
    val niNumber: String? = null,
    val streetName: String = "8 Ripon Lane",
    val town: String = "Sheffield",
    val county: String = "South Yorkshire",
    val postcode: String = "S29 1TT",
    val offenderManagers: List<OffenderManagerReplacement> = listOf(OffenderManagerReplacement())
)

data class AliasReplacement(
    val surname: String,
    val firstName: String,
    val dateOfBirth: LocalDate
)

data class OffenderManagerReplacement(
    val code: String = "N02",
    val description: String = "NPS North East",
    val active: Boolean = true
)