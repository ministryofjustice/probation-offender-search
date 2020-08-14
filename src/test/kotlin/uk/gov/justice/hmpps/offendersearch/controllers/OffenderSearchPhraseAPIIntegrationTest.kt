package uk.gov.justice.hmpps.offendersearch.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import io.restassured.RestAssured
import org.elasticsearch.client.RestHighLevelClient
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.hmpps.offendersearch.dto.OffenderAlias
import uk.gov.justice.hmpps.offendersearch.dto.OffenderDetail
import uk.gov.justice.hmpps.offendersearch.dto.ProbationArea
import uk.gov.justice.hmpps.offendersearch.dto.SearchPhraseFilter
import uk.gov.justice.hmpps.offendersearch.util.JwtAuthenticationHelper
import uk.gov.justice.hmpps.offendersearch.util.LocalStackHelper
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["test", "localstack"])
class OffenderSearchPhraseAPIIntegrationTest {
  @Autowired
  internal lateinit var jwtAuthenticationHelper: JwtAuthenticationHelper

  @Autowired
  @Qualifier("elasticSearchClient")
  private lateinit var esClient: RestHighLevelClient

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Value("\${search.supported.mapping.version}")
  private lateinit var mappingVersion: String

  @Value("\${local.server.port}")
  private var port: Int = 0

  @BeforeEach
  internal fun before() {
    RestAssured.port = port
  }

  @Nested
  inner class SmokeTest {
    @Test
    internal fun `a very basic surname search works`() {
      loadOffenders(
          OffenderReplacement(crn = "X00001"),
          OffenderReplacement(
              surname = "Gramsci",
              crn = "X99999"
          ))

      hasSingleMatch(phrase = "gramsci", crn = "X99999")
    }
  }

  @Nested
  @Disabled("waiting for implementation")
  @TestInstance(PER_CLASS)
  inner class SingleTermMatching {
    @Suppress("unused")
    fun matchAllTerms() = listOf(
        Arguments.of(false),
        Arguments.of(true)
    )

    @BeforeAll
    internal fun setUp() {
      loadOffenders(
          OffenderReplacement(crn = "X00001"),
          OffenderReplacement(
              surname = "Gramsci",
              firstName = "Anne",
              gender = "Female",
              middleNames = listOf("Jane", "Jo"),
              dateOfBirth = LocalDate.parse("1988-01-06"),
              crn = "X99999",
              nomsNumber = "G5555TT",
              croNumber = "SF80/655108T",
              pncNumber = "2018/0123456X",
              niNumber = "NE112233X",
              aliases = listOf(AliasReplacement(surname = "Mouse", firstName = "Mini")),
              streetName = "23 Hyde Park Road",
              town = "Southampton",
              county = "Hampshire",
              postcode = "H1 1WA",
              offenderManagers = listOf(OffenderManagerReplacement("N03", "NPS London"))
          ))
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by surname`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "gramsci", crn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by first name`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "Anne", crn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by any middle name`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "Jane", crn = "X99999", matchAllTerms = matchAllTerms)
      hasSingleMatch(phrase = "Jo", crn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by date of birth`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "1988-01-06", crn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by gender`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "Female", crn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by crn`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "X99999", crn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by noms number`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "G5555TT", crn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by pnc number`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "2018/0123456X", crn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by cro number`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "SF80/655108T", crn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by national insurance number`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "NE112233X", crn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by partial street name`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "Hyde", crn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by town name`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "Southampton", crn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by Hampshire name`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "Southampton", crn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by post code name`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "H1 1WA", crn = "X99999", matchAllTerms = matchAllTerms)
    }
  }

  private fun hasSingleMatch(phrase: String, @Suppress("SameParameterValue") crn: String, matchAllTerms: Boolean = false) {
    RestAssured.given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body(SearchPhraseFilter(
            phrase = phrase,
            matchAllTerms = matchAllTerms
        ))
        .post("/phrase")
        .then()
        .statusCode(200)
        .body("total", equalTo(1))
        .body("offenders[0].otherIds.crn", equalTo(crn))
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
    val dateOfBirth: LocalDate = LocalDate.parse("1965-07-18")
)

data class OffenderManagerReplacement(
    val code: String = "N02",
    val description: String = "NPS North East",
    val active: Boolean = true
)