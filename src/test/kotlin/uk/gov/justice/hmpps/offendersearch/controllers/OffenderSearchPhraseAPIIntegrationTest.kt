package uk.gov.justice.hmpps.offendersearch.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import io.restassured.RestAssured
import io.restassured.response.ValidatableResponse
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
import java.time.format.DateTimeFormatter
import kotlin.random.Random

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

      hasSingleMatch(phrase = "gramsci", expectedCrn = "X99999")
    }
  }

  @Nested
  @Disabled("waiting for implementation")
  @TestInstance(PER_CLASS)
  inner class SingleTermMatching {
    @Suppress("unused")
    fun matchAllTerms() = listOf(false, true)

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
      hasSingleMatch(phrase = "gramsci", expectedCrn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by first name`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "Anne", expectedCrn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by any middle name`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "Jane", expectedCrn = "X99999", matchAllTerms = matchAllTerms)
      hasSingleMatch(phrase = "Jo", expectedCrn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by date of birth`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "1988-01-06", expectedCrn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by gender`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "Female", expectedCrn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by crn`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "X99999", expectedCrn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by noms number`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "G5555TT", expectedCrn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by pnc number`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "2018/0123456X", expectedCrn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by cro number`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "SF80/655108T", expectedCrn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by national insurance number`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "NE112233X", expectedCrn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by partial street name`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "Hyde", expectedCrn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by town name`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "Southampton", expectedCrn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by county name`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "Hampshire", expectedCrn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by post code name`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "H1 1WA", expectedCrn = "X99999", matchAllTerms = matchAllTerms)
    }
  }

  @Nested
  @Disabled("waiting for implementation")
  @TestInstance(PER_CLASS)
  inner class MatchingMultipleOffenders {
    @Suppress("unused")
    fun matchAllTerms() = listOf(false, true)

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
          ),
          OffenderReplacement(
              surname = "Gramsci",
              firstName = "Anne",
              gender = "Female",
              middleNames = listOf("Jane", "Jo"),
              dateOfBirth = LocalDate.parse("1988-01-06"),
              crn = "X88888",
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
      hasMatches(phrase = "Gramsci", matchAllTerms = matchAllTerms, expectedCrns = listOf("X99999", "X88888"))
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by first name`(matchAllTerms: Boolean) {
      hasMatches(phrase = "Anne", matchAllTerms = matchAllTerms, expectedCrns = listOf("X99999", "X88888"))
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by any middle name`(matchAllTerms: Boolean) {
      hasMatches(phrase = "Jane", matchAllTerms = matchAllTerms, expectedCrns = listOf("X99999", "X88888"))
      hasMatches(phrase = "Jo", matchAllTerms = matchAllTerms, expectedCrns = listOf("X99999", "X88888"))
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by date of birth`(matchAllTerms: Boolean) {
      hasMatches(phrase = "1988-01-06", expectedCrns = listOf("X99999", "X88888"), matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by gender`(matchAllTerms: Boolean) {
      hasMatches(phrase = "Female", expectedCrns = listOf("X99999", "X88888"), matchAllTerms = matchAllTerms)
    }

    @Test
    internal fun `can match by crn`() {
      hasMatches(phrase = "X99999 X88888", expectedCrns = listOf("X99999", "X88888"), matchAllTerms = false)
      hasNoMatch(phrase = "X99999 X88888", matchAllTerms = true)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by noms number`(matchAllTerms: Boolean) {
      hasMatches(phrase = "G5555TT", expectedCrns = listOf("X99999", "X88888"), matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by pnc number`(matchAllTerms: Boolean) {
      hasMatches(phrase = "2018/0123456X", expectedCrns = listOf("X99999", "X88888"), matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by cro number`(matchAllTerms: Boolean) {
      hasMatches(phrase = "SF80/655108T", expectedCrns = listOf("X99999", "X88888"), matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by national insurance number`(matchAllTerms: Boolean) {
      hasMatches(phrase = "NE112233X", expectedCrns = listOf("X99999", "X88888"), matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by partial street name`(matchAllTerms: Boolean) {
      hasMatches(phrase = "Hyde", expectedCrns = listOf("X99999", "X88888"), matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by town name`(matchAllTerms: Boolean) {
      hasMatches(phrase = "Southampton", expectedCrns = listOf("X99999", "X88888"), matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by county name`(matchAllTerms: Boolean) {
      hasMatches(phrase = "Hampshire", expectedCrns = listOf("X99999", "X88888"), matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by post code name`(matchAllTerms: Boolean) {
      hasMatches(phrase = "H1 1WA", expectedCrns = listOf("X99999", "X88888"), matchAllTerms = matchAllTerms)
    }
  }

  @Nested
  @Disabled("waiting for implementation")
  @TestInstance(PER_CLASS)
  inner class MultipleTermTermMatching {
    @Suppress("unused")
    fun matchAllTerms() = listOf(false, true)

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
      hasSingleMatch(phrase = "gramsci X99999", expectedCrn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by first name`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "Anne X99999", expectedCrn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by any middle name`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "Jane X99999", expectedCrn = "X99999", matchAllTerms = matchAllTerms)
      hasSingleMatch(phrase = "Jo X99999", expectedCrn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by date of birth`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "1988-01-06 X99999", expectedCrn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by gender`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "Female X99999", expectedCrn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by noms number`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "G5555TT X99999", expectedCrn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by pnc number`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "2018/0123456X X99999", expectedCrn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by cro number`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "SF80/655108T X99999", expectedCrn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by national insurance number`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "NE112233X X99999", expectedCrn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by partial street name`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "Hyde X99999", expectedCrn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by town name`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "Southampton X99999", expectedCrn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by county name`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "Hampshire X99999", expectedCrn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by post code name`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "H1 1WA X99999", expectedCrn = "X99999", matchAllTerms = matchAllTerms)
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can match by all terms`(matchAllTerms: Boolean) {
      hasSingleMatch(phrase = "gramsci Anne Jane Jo 1988-01-06 Female X99999 G5555TT 2018/0123456X SF80/655108T NE112233X Hyde Southampton Hampshire H1 1WA", expectedCrn = "X99999", matchAllTerms = matchAllTerms)
    }
  }

  @Nested
  @Disabled("waiting for implementation")
  @TestInstance(PER_CLASS)
  inner class DateOfBirthMatching {
    @BeforeAll
    internal fun setUp() {
      loadOffenders(
          OffenderReplacement(crn = "X00001"),
          OffenderReplacement(
              dateOfBirth = LocalDate.parse("1988-01-06"),
              crn = "X99999"
          ))
    }

    @Suppress("unused")
    fun dateOfBirthFormats() = listOf(
        "yyyy/MMMM/dd",
        "yyyy-MMMM-dd",
        "yyyy/MMMM/d",
        "yyyy-MMMM-d",
        "yyyy/MMM/dd",
        "yyyy-MMM-dd",
        "yyyy/MMM/d",
        "yyyy-MMM-d",
        "yyyy/MM/dd",
        "yyyy-MM-dd",
        "yyyy/M/dd",
        "yyyy-M-dd",
        "yyyy/MM/d",
        "yyyy-MM-d",
        "yyyy/M/d",
        "yyyy-M-d",
        "dd/MMMM/yyyy",
        "dd-MMMM-yyyy",
        "d/MMMM/yyyy",
        "d-MMMM-yyyy",
        "dd/MMM/yyyy",
        "dd-MMM-yyyy",
        "d/MMM/yyyy",
        "d-MMM-yyyy",
        "dd/MM/yyyy",
        "dd-MM-yyyy",
        "dd/M/yyyy",
        "dd-M-yyyy",
        "d/MM/yyyy",
        "d-MM-yyyy",
        "d/M/yyyy",
        "d-M-yyyy"
    ).map { LocalDate.parse("1988-01-06").format(DateTimeFormatter.ofPattern(it)) }


    @ParameterizedTest
    @MethodSource("dateOfBirthFormats")
    internal fun `can match by date of birth in various formats`(dateOfBirth: String) {
      hasSingleMatch(phrase = dateOfBirth, expectedCrn = "X99999")
    }
  }

  @Nested
  @Disabled("waiting for implementation")
  @TestInstance(PER_CLASS)
  inner class PNCNumberMatching {
    @BeforeAll
    internal fun setUp() {
      loadOffenders(
          OffenderReplacement(crn = "X00001"),
          OffenderReplacement(
              crn = "X99999",
              pncNumber = "2018/0023456X"
          ))
    }

    @Suppress("unused")
    fun pncNumberFormats() = listOf(
        "2018/0023456X",
        "2018/0023456x",
        "2018/023456X",
        "2018/23456X",
        "18/0023456X",
        "18/0023456x",
        "18/023456X",
        "18/23456X")


    @ParameterizedTest
    @MethodSource("pncNumberFormats")
    internal fun `can match by pnc number in various formats`(pncNumber: String) {
      hasSingleMatch(phrase = pncNumber, expectedCrn = "X99999")
    }
  }

  @Nested
  @Disabled("waiting for implementation")
  @TestInstance(PER_CLASS)
  inner class FirstNamePrefixMatching {
    @BeforeAll
    internal fun setUp() {
      @Suppress("SameParameterValue")
      loadOffenders(
          OffenderReplacement(crn = "X00001", firstName = "Xabier"),
          OffenderReplacement(
              crn = "X99999",
              surname = "Gramsci",
              firstName = "Antonio"
          ))
    }

    @Suppress("unused")
    fun firstNamePrefixes() = listOf(
        "Antoni",
        "Anton",
        "Anto",
        "Ant",
        "An",
        "A")


    @ParameterizedTest
    @MethodSource("firstNamePrefixes")
    internal fun `can match first name using partial name but only when not matching all terms`(firstName: String) {
      hasSingleMatch(phrase = firstName, expectedCrn = "X99999", matchAllTerms = false)
      hasNoMatch(phrase = firstName, matchAllTerms = true)
    }
  }

  @Nested
  @Disabled("waiting for implementation")
  @TestInstance(PER_CLASS)
  inner class DeletedOffenders {
    @Suppress("unused")
    fun matchAllTerms() = listOf(false, true)

    @BeforeAll
    internal fun setUp() {
      loadOffenders(
          OffenderReplacement(
              crn = "X99999",
              deleted = false
          ),
          OffenderReplacement(
              crn = "X88888",
              deleted = true
          ))
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `will not find deleted offenders`(matchAllTerms: Boolean) {
      hasNoMatch(phrase = "X99999", matchAllTerms = matchAllTerms)
      hasSingleMatch(phrase = "X88888", expectedCrn = "X99999", matchAllTerms = matchAllTerms)
    }
  }

  @Nested
  @TestInstance(PER_CLASS)
  inner class Paging {
    @BeforeAll
    internal fun loadLotsOfOffenders() {
      val offenders = (1..101).map {
        OffenderReplacement(offenderId = it.toLong(), crn = it.toCrn(), firstName = "Antonio", surname = "Gramsci")
      }.toTypedArray()
      loadOffenders(*offenders)
    }

    @Test
    internal fun `by default will return a page of 10 offenders along with total`() {
      doSearch("antonio gramsci")
          .body("offenders.size()", equalTo(10))
          .body("total", equalTo(101))
    }
    @Test
    internal fun `can specify page size`() {
      doSearch("antonio gramsci", size = 20)
          .body("offenders.size()", equalTo(20))
          .body("total", equalTo(101))
    }
    @Test
    internal fun `when results are identical order will be by offender id desc`() {
      doSearch("antonio gramsci", size = 101, page = 0)
          .body("offenders.size()", equalTo(101))
          .body("offenders[0].offenderId", equalTo(101))
          .body("offenders[1].offenderId", equalTo(100))
          .body("offenders[99].offenderId", equalTo(2))
          .body("offenders[100].offenderId", equalTo(1))
          .body("total", equalTo(101))
    }
    @Test
    internal fun `can page through the results`() {
      doSearch("antonio gramsci", size = 10)
          .body("total", equalTo(101))
          .body("offenders.size()", equalTo(10))
          .body("offenders[0].otherIds.crn", equalTo("X00101"))
          .body("offenders[9].otherIds.crn", equalTo("X00092"))
      doSearch("antonio gramsci", size = 10, page = 1)
          .body("total", equalTo(101))
          .body("offenders.size()", equalTo(10))
          .body("offenders[0].otherIds.crn", equalTo("X00091"))
          .body("offenders[9].otherIds.crn", equalTo("X00082"))
      doSearch("antonio gramsci", size = 10, page = 10)
          .body("total", equalTo(101))
          .body("offenders.size()", equalTo(1))
          .body("offenders[0].otherIds.crn", equalTo("X00001"))
    }
  }

  private fun hasSingleMatch(phrase: String, @Suppress("SameParameterValue") expectedCrn: String, matchAllTerms: Boolean = false) {
    doSearch(phrase = phrase, matchAllTerms = matchAllTerms)
        .body("total", equalTo(1))
        .body("offenders[0].otherIds.crn", equalTo(expectedCrn))
  }

  private fun hasMatches(phrase: String, matchAllTerms: Boolean = false, expectedCrns: List<String>) {
    val response = doSearch(phrase = phrase, matchAllTerms = matchAllTerms)

    expectedCrns.forEach {
      response
          .body("offenders.find { it.otherIds.crn == \"$it\" }.otherIds.crn", equalTo(it))
    }

    response
        .body("total", equalTo(expectedCrns.size))
  }

  private fun hasNoMatch(phrase: String, matchAllTerms: Boolean = false) {
    doSearch(phrase = phrase, matchAllTerms = matchAllTerms)
        .body("total", equalTo(0))
  }

  private fun doSearch(phrase: String, matchAllTerms: Boolean = false, size: Int? = null, page: Int? = null): ValidatableResponse {
    val filter = SearchPhraseFilter(
        phrase = phrase,
        matchAllTerms = matchAllTerms
    ).let {filter ->
      size?.let {filter.copy(size = it)}?:filter
    }.let {filter ->
      page?.let {filter.copy(page = it)}?:filter
    }

    return RestAssured.given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body(filter)
        .post("/phrase")
        .then()
        .statusCode(200)
  }

  fun loadOffenders(vararg offenders: OffenderReplacement) {
    val template = "/elasticsearchdata/offender-template.json".readResourceAsText()
    val templateOffender = objectMapper.readValue(template, OffenderDetail::class.java)

    val offendersToLoad = offenders.map {
      templateOffender.copy(
          offenderId = it.offenderId,
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
    val offenderId: Long = Random(0).nextLong(),
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

fun Int.toCrn() = "X%05d".format(this)
