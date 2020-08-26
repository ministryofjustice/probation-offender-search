package uk.gov.justice.hmpps.offendersearch.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.nhaarman.mockito_kotlin.verify
import io.restassured.RestAssured
import io.restassured.response.ValidatableResponse
import org.elasticsearch.client.RestHighLevelClient
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.extension.ExtendWith
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
import uk.gov.justice.hmpps.offendersearch.util.JwtAuthenticationHelper.ClientUser
import uk.gov.justice.hmpps.offendersearch.util.LocalStackHelper
import uk.gov.justice.hmpps.offendersearch.wiremock.CommunityApiExtension
import uk.gov.justice.hmpps.offendersearch.wiremock.OAuthExtension
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random

@ExtendWith(OAuthExtension::class, CommunityApiExtension::class)
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
              middleNames = listOf("Jane", "Joanna"),
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
      hasSingleMatch(phrase = "Joanna", expectedCrn = "X99999", matchAllTerms = matchAllTerms)
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
              middleNames = listOf("Jane", "Joanna"),
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
              middleNames = listOf("Jane", "Joanna"),
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
      hasMatches(phrase = "Joanna", matchAllTerms = matchAllTerms, expectedCrns = listOf("X99999", "X88888"))
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
              middleNames = listOf("Jane", "Joanna"),
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
      hasSingleMatch(phrase = "Joanna X99999", expectedCrn = "X99999", matchAllTerms = matchAllTerms)
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
      hasSingleMatch(phrase = "gramsci Anne Jane Joanna 1988-01-06 Female X99999 G5555TT 2018/0123456X SF80/655108T NE112233X Hyde Southampton Hampshire H1 1WA", expectedCrn = "X99999", matchAllTerms = matchAllTerms)
    }
  }

  @Nested
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
        "An")


    @ParameterizedTest
    @MethodSource("firstNamePrefixes")
    internal fun `can match first name using partial name but only when not matching all terms`(firstName: String) {
      hasSingleMatch(phrase = firstName, expectedCrn = "X99999", matchAllTerms = false)
      hasNoMatch(phrase = firstName, matchAllTerms = true)
    }

    @Test
    internal fun `can match first name using first letter name`() {
      hasSingleMatch(phrase = "A", expectedCrn = "X99999", matchAllTerms = false)
    }

    @Test
    // we think this is bug, but we have copied like for like existing functionality from the Newtech application
    internal fun `will (incorrectly) return all offenders when searching by single letter when matching all terms`() {
      hasMatches(phrase = "A", expectedCrns = listOf("X99999", "X00001"), matchAllTerms = true)
    }

  }

  @Nested
  @TestInstance(PER_CLASS)
  inner class DeletedOffenders {
    @Suppress("unused")
    fun matchAllTerms() = listOf(false, true)

    @BeforeAll
    internal fun setUp() {
      loadOffenders(
          OffenderReplacement(
              crn = "X99999",
              deleted = true
          ),
          OffenderReplacement(
              crn = "X88888",
              deleted = false
          ))
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `will not find deleted offenders`(matchAllTerms: Boolean) {
      hasNoMatch(phrase = "X99999", matchAllTerms = matchAllTerms)
      hasSingleMatch(phrase = "X88888", expectedCrn = "X88888", matchAllTerms = matchAllTerms)
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
    internal fun `by default will return a page of 10 offenders along with totals`() {
      doSearch("antonio gramsci")
          .body("content.size()", equalTo(10))
          .body("size", equalTo(10))
          .body("numberOfElements", equalTo(10))
          .body("totalElements", equalTo(101))
          .body("totalPages", equalTo(11))
    }

    @Test
    internal fun `can specify page size`() {
      doSearch("antonio gramsci", size = 20)
          .body("content.size()", equalTo(20))
          .body("numberOfElements", equalTo(20))
          .body("totalElements", equalTo(101))
    }

    @Test
    internal fun `when results are identical order will be by offender id desc`() {
      doSearch("antonio gramsci", size = 101, page = 0)
          .body("content.size()", equalTo(101))
          .body("content[0].offenderId", equalTo(101))
          .body("content[1].offenderId", equalTo(100))
          .body("content[99].offenderId", equalTo(2))
          .body("content[100].offenderId", equalTo(1))
          .body("totalElements", equalTo(101))
    }

    @Test
    internal fun `can page through the results`() {
      doSearch("antonio gramsci", size = 10)
          .body("totalElements", equalTo(101))
          .body("content.size()", equalTo(10))
          .body("numberOfElements", equalTo(10))
          .body("size", equalTo(10))
          .body("number", equalTo(0))
          .body("content[0].otherIds.crn", equalTo("X00101"))
          .body("content[9].otherIds.crn", equalTo("X00092"))
      doSearch("antonio gramsci", size = 10, page = 1)
          .body("totalElements", equalTo(101))
          .body("content.size()", equalTo(10))
          .body("numberOfElements", equalTo(10))
          .body("size", equalTo(10))
          .body("number", equalTo(1))
          .body("content[0].otherIds.crn", equalTo("X00091"))
          .body("content[9].otherIds.crn", equalTo("X00082"))
      doSearch("antonio gramsci", size = 10, page = 10)
          .body("totalElements", equalTo(101))
          .body("content.size()", equalTo(1))
          .body("numberOfElements", equalTo(1))
          .body("size", equalTo(10))
          .body("number", equalTo(10))
          .body("content[0].otherIds.crn", equalTo("X00001"))
    }
  }

  @Nested
  @TestInstance(PER_CLASS)
  inner class Suggestions {
    @BeforeAll
    internal fun loadLotsOfOffenders() {
      val offenders = (1..10).map {
        OffenderReplacement(offenderId = it.toLong(), crn = it.toCrn(), firstName = "Fred", surname = "Jones")
      }.toTypedArray()
      loadOffenders(*offenders)
    }

    @Test
    internal fun `should not get any suggestions when we have it correct with no other offenders`() {
      doSearch("fred jones")
          .body("suggestions.suggest.firstName.find { it.text == \"fred\" }.options.size()", equalTo(0))
          .body("suggestions.suggest.firstName.find { it.text == \"jones\" }.options.size()", equalTo(0))
          .body("suggestions.suggest.surname.find { it.text == \"fred\" }.options.size()", equalTo(0))
          .body("suggestions.suggest.surname.find { it.text == \"jones\" }.options.size()", equalTo(0))
    }

    @Test
    internal fun `will get first name suggestions when nearly correct`() {
      doSearch("frod jones")
          .body("suggestions.suggest.firstName.find { it.text == \"frod\" }.options.size()", equalTo(1))
          .body("suggestions.suggest.firstName.find { it.text == \"frod\" }.options[0].text", equalTo("fred"))
          .body("suggestions.suggest.firstName.find { it.text == \"jones\" }.options.size()", equalTo(0))
          .body("suggestions.suggest.surname.find { it.text == \"frod\" }.options.size()", equalTo(0))
          .body("suggestions.suggest.surname.find { it.text == \"jones\" }.options.size()", equalTo(0))
    }

    @Test
    internal fun `will get surname suggestions when nearly correct`() {
      doSearch("fred janes")
          .body("suggestions.suggest.firstName.find { it.text == \"fred\" }.options.size()", equalTo(0))
          .body("suggestions.suggest.firstName.find { it.text == \"janes\" }.options.size()", equalTo(0))
          .body("suggestions.suggest.surname.find { it.text == \"fred\" }.options.size()", equalTo(0))
          .body("suggestions.suggest.surname.find { it.text == \"janes\" }.options.size()", equalTo(1))
          .body("suggestions.suggest.surname.find { it.text == \"janes\" }.options[0].text", equalTo("jones"))
    }

    @Test
    internal fun `can get suggestions for both first name and surname at the same time`() {
      doSearch("frod janes")
          .body("suggestions.suggest.firstName.find { it.text == \"frod\" }.options.size()", equalTo(1))
          .body("suggestions.suggest.firstName.find { it.text == \"frod\" }.options[0].text", equalTo("fred"))
          .body("suggestions.suggest.firstName.find { it.text == \"janes\" }.options.size()", equalTo(0))
          .body("suggestions.suggest.surname.find { it.text == \"frod\" }.options.size()", equalTo(0))
          .body("suggestions.suggest.surname.find { it.text == \"janes\" }.options.size()", equalTo(1))
          .body("suggestions.suggest.surname.find { it.text == \"janes\" }.options[0].text", equalTo("jones"))
    }
  }

  @Nested
  @TestInstance(PER_CLASS)
  inner class WeightingWithOrdering {
    @Nested
    @TestInstance(PER_CLASS)
    inner class MainNames {
      @Suppress("unused")
      fun matchAllTerms() = listOf(false, true)

      @BeforeAll
      internal fun loadLotsOfOffenders() {
        loadOffenders(
            OffenderReplacement(
                offenderId = 1,
                crn = "X00001",
                aliases = listOf(AliasReplacement(firstName = "Fred", surname = "Jones")),
                firstName = "Antonio",
                surname = "Gramsci"
            ),
            OffenderReplacement(
                offenderId = 2,
                crn = "X00002",
                aliases = listOf(AliasReplacement(firstName = "Antonio", surname = "Gramsci")),
                surname = "Jones",
                firstName = "Fred"
            ),
            OffenderReplacement(
                offenderId = 3,
                crn = "X00003",
                aliases = listOf(AliasReplacement(firstName = "Antonio", surname = "Jones")),
                firstName = "Fred",
                surname = "Gramsci"
            ),
            OffenderReplacement(
                offenderId = 4,
                crn = "X00004",
                aliases = listOf(AliasReplacement(firstName = "Fred", surname = "Gramsci")),
                firstName = "Antonio",
                surname = "Jones"
            )
        )
      }


      @Test
      internal fun `offender alias surname has lower ranking than real surname when matching all terms`() {
        doSearch("antonio gramsci", true)
            .body("content.size()", equalTo(4))
            .body("content[0].otherIds.crn", equalTo("X00001"))
            .body("content[1].otherIds.crn", equalTo("X00004"))
            .body("content[2].otherIds.crn", equalTo("X00003"))
            .body("content[3].otherIds.crn", equalTo("X00002"))

        doSearch("fred jones", true)
            .body("content.size()", equalTo(4))
            .body("content[0].otherIds.crn", equalTo("X00002"))
            .body("content[1].otherIds.crn", equalTo("X00004"))
            .body("content[2].otherIds.crn", equalTo("X00003"))
            .body("content[3].otherIds.crn", equalTo("X00001"))
      }

      @Test
      // this test shows what we think is a bug; whereby when first name it supplied it is matched twice in the cross field query
      // and the prefix query. This artificially boosts it's importance. Since this is a like for like conversion of the exitsing
      // NewTech algorithum this replicates that "feature"
      internal fun `first name is (incorrectly) boosted higher than surname when not matching all terms due to the additional prefix matching`() {
        doSearch("antonio gramsci", false)
            .body("content.size()", equalTo(4))
            .body("content[0].otherIds.crn", equalTo("X00001"))
            .body("content[1].otherIds.crn", equalTo("X00004"))
            .body("content[2].otherIds.crn", equalTo("X00003"))
            .body("content[3].otherIds.crn", equalTo("X00002"))

        doSearch("fred jones", false)
            .body("content.size()", equalTo(4))
            .body("content[0].otherIds.crn", equalTo("X00002"))
            .body("content[1].otherIds.crn", equalTo("X00003"))
            .body("content[2].otherIds.crn", equalTo("X00004"))
            .body("content[3].otherIds.crn", equalTo("X00001"))
      }

      @ParameterizedTest
      @MethodSource("matchAllTerms")
      internal fun `offender alias first name has lower ranking than real first name`(matchAllTerms: Boolean) {
        doSearch("fred gramsci", matchAllTerms)
            .body("content.size()", equalTo(4))
            .body("content[0].otherIds.crn", equalTo("X00003"))

        doSearch("antonio jones", matchAllTerms)
            .body("content.size()", equalTo(4))
            .body("content[0].otherIds.crn", equalTo("X00004"))
      }
    }

    @Nested
    @TestInstance(PER_CLASS)
    inner class MiddleNames {
      @Suppress("unused")
      fun matchAllTerms() = listOf(false, true)

      @BeforeAll
      internal fun loadLotsOfOffenders() {
        loadOffenders(
            OffenderReplacement(
                offenderId = 1,
                crn = "X00001",
                firstName = "Antonio",
                middleNames = listOf("Fred"),
                surname = "Gramsci"
            ),
            OffenderReplacement(
                offenderId = 2,
                crn = "X00002",
                firstName = "Fred",
                middleNames = listOf("Antonio"),
                surname = "Gramsci"
            )
        )
      }

      @ParameterizedTest
      @MethodSource("matchAllTerms")
      internal fun `offender middle name has lower ranking than first name`(matchAllTerms: Boolean) {
        doSearch("antonio gramsci", matchAllTerms)
            .body("content.size()", equalTo(2))
            .body("content[0].otherIds.crn", equalTo("X00001"))
            .body("content[1].otherIds.crn", equalTo("X00002"))

        doSearch("fred gramsci", matchAllTerms)
            .body("content.size()", equalTo(2))
            .body("content[0].otherIds.crn", equalTo("X00002"))
            .body("content[1].otherIds.crn", equalTo("X00001"))
      }
    }

    @Nested
    @TestInstance(PER_CLASS)
    inner class Address {
      @Suppress("unused")
      fun matchAllTerms() = listOf(false, true)

      @BeforeAll
      internal fun loadLotsOfOffenders() {
        loadOffenders(
            OffenderReplacement(
                offenderId = 1,
                crn = "X00001",
                surname = "Gramsci",
                streetName = "1 Main Street",
                postcode = "S10 2BJ"
            ),
            OffenderReplacement(
                offenderId = 2,
                crn = "X00002",
                surname = "Gramsci",
                streetName = "9 High Road",
                postcode = "N17 3NH"
            )
        )
      }

      @Test
      internal fun `address postcode has higher ranking then street name`() {
        doSearch("gramsci Main", false)
            .body("content.size()", equalTo(2))
            .body("content[0].otherIds.crn", equalTo("X00001"))
            .body("content[1].otherIds.crn", equalTo("X00002"))

        doSearch("gramsci N17 3NH Main", false)
            .body("content.size()", equalTo(2))
            .body("content[0].otherIds.crn", equalTo("X00002"))
            .body("content[1].otherIds.crn", equalTo("X00001"))
      }
    }

    @Nested
    @TestInstance(PER_CLASS)
    inner class DateOfBirth {
      @Suppress("unused")
      fun matchAllTerms() = listOf(false, true)

      @BeforeAll
      internal fun loadLotsOfOffenders() {
        loadOffenders(
            OffenderReplacement(
                offenderId = 1,
                crn = "X00001",
                firstName = "Antonio",
                surname = "Gramsci",
                dateOfBirth = LocalDate.parse("1987-01-29")
            ),
            OffenderReplacement(
                offenderId = 2,
                crn = "X00002",
                firstName = "Antonio",
                surname = "Smith",
                dateOfBirth = LocalDate.parse("1984-02-21")
            )
        )
      }

      @Test
      internal fun `date of birth has higher ranking than names`() {
        doSearch("antonio smith 1987-01-29", false)
            .body("content.size()", equalTo(2))
            .body("content[0].otherIds.crn", equalTo("X00001"))
            .body("content[1].otherIds.crn", equalTo("X00002"))

        doSearch("antonio gramsci 1984-02-21", false)
            .body("content.size()", equalTo(2))
            .body("content[0].otherIds.crn", equalTo("X00002"))
            .body("content[1].otherIds.crn", equalTo("X00001"))
      }
    }

    @Nested
    @TestInstance(PER_CLASS)
    inner class IDs {
      @Suppress("unused")
      fun matchAllTerms() = listOf(false, true)

      @BeforeAll
      internal fun loadLotsOfOffenders() {
        loadOffenders(
            OffenderReplacement(
                offenderId = 1,
                crn = "X00001",
                aliases = listOf(AliasReplacement(firstName = "Antonio", surname = "gramsci")),
                pncNumber = "2019/1X",
                croNumber = "123456/99a",
                nomsNumber = "A1234X"
            ),
            OffenderReplacement(
                offenderId = 2,
                crn = "X00002",
                aliases = listOf(AliasReplacement(firstName = "Antonio", surname = "gramsci"))
            )
        )
      }

      @Test
      internal fun `sorted by offender id when scores the same`() {
        doSearch("antonio smith", false)
            .body("content.size()", equalTo(2))
            .body("content[0].otherIds.crn", equalTo("X00002"))
            .body("content[1].otherIds.crn", equalTo("X00001"))
      }

      @Test
      internal fun `pnc number boosts order`() {
        doSearch("antonio smith 2019/1X", false)
            .body("content.size()", equalTo(2))
            .body("content[0].otherIds.crn", equalTo("X00001"))
            .body("content[1].otherIds.crn", equalTo("X00002"))
      }

      @Test
      internal fun `cro number boosts order`() {
        doSearch("antonio smith 123456/99A", false)
            .body("content.size()", equalTo(2))
            .body("content[0].otherIds.crn", equalTo("X00001"))
            .body("content[1].otherIds.crn", equalTo("X00002"))
      }

      @Test
      internal fun `noms number boosts order`() {
        doSearch("antonio smith A1234X", false)
            .body("content.size()", equalTo(2))
            .body("content[0].otherIds.crn", equalTo("X00001"))
            .body("content[1].otherIds.crn", equalTo("X00002"))
      }

      @Test
      internal fun `crn boosts order`() {
        doSearch("antonio smith X00001", false)
            .body("content.size()", equalTo(2))
            .body("content[0].otherIds.crn", equalTo("X00001"))
            .body("content[1].otherIds.crn", equalTo("X00002"))

        doSearch("antonio smith X00002", false)
            .body("content.size()", equalTo(2))
            .body("content[0].otherIds.crn", equalTo("X00002"))
            .body("content[1].otherIds.crn", equalTo("X00001"))
      }
    }
  }

  @Nested
  @TestInstance(PER_CLASS)
  inner class AggregationAndFiltering {
    @Suppress("unused")
    fun matchAllTerms() = listOf(false, true)

    @BeforeAll
    internal fun loadOffendersDistributedInAreas() {
      loadOffenders(
          OffenderReplacement(
              crn = "X00001",
              surname = "Gramsci",
              offenderManagers = listOf(OffenderManagerReplacement(code = "N01", description = "NPS North West", active = true))
          ),
          OffenderReplacement(
              crn = "X00002",
              surname = "Gramsci",
              offenderManagers = listOf(OffenderManagerReplacement(code = "N01", description = "NPS North West", active = true))
          ),
          OffenderReplacement(
              crn = "X00003",
              surname = "Gramsci",
              offenderManagers = listOf(OffenderManagerReplacement(code = "N01", description = "NPS North West", active = true))
          ),
          OffenderReplacement(
              crn = "X00004",
              surname = "Gramsci",
              offenderManagers = listOf(OffenderManagerReplacement(code = "N02", description = "NPS North East", active = true))
          ),
          OffenderReplacement(
              crn = "X00005",
              surname = "Smith",
              offenderManagers = listOf(OffenderManagerReplacement(code = "N03", description = "NPS Midlands", active = true))
          ),
          OffenderReplacement(
              crn = "X00006",
              surname = "Gramsci",
              offenderManagers = listOf(OffenderManagerReplacement(code = "N07", description = "NPS London", active = true))
          ),
          OffenderReplacement(
              crn = "X00007",
              surname = "Gramsci",
              offenderManagers = listOf(
                  OffenderManagerReplacement(code = "N01", description = "NPS North West", active = false),
                  OffenderManagerReplacement(code = "N07", description = "NPS London", active = true)
              )
          )
      )
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `will aggregate by active offender manager area ordered by count descending`(matchAllTerms: Boolean) {
      doSearch("Gramsci", matchAllTerms)
          .body("content.size()", equalTo(6))
          .body("probationAreaAggregations.size()", CoreMatchers.equalTo(3))

          .body("probationAreaAggregations[0].code", CoreMatchers.equalTo("N01"))
          .body("probationAreaAggregations[0].count", CoreMatchers.equalTo(3))

          .body("probationAreaAggregations[1].code", CoreMatchers.equalTo("N07"))
          .body("probationAreaAggregations[1].count", CoreMatchers.equalTo(2))

          .body("probationAreaAggregations[2].code", CoreMatchers.equalTo("N02"))
          .body("probationAreaAggregations[2].count", CoreMatchers.equalTo(1))
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can filter by probation area`(matchAllTerms: Boolean) {
      hasMatches("Gramsci", matchAllTerms, listOf("X00001", "X00002", "X00003"), filter = listOf("N01"))
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `can filter by more than one probation area`(matchAllTerms: Boolean) {
      hasMatches("Gramsci", matchAllTerms, listOf("X00001", "X00002", "X00003", "X00004"), filter = listOf("N01", "N02"))
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `empty filter should return all matching results`(matchAllTerms: Boolean) {
      hasMatches("Gramsci", matchAllTerms, listOf("X00001", "X00002", "X00003", "X00004", "X00006", "X00007"), filter = listOf())
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `will return aggregations for all areas even when filtered`(matchAllTerms: Boolean) {
      doSearch("Gramsci", matchAllTerms, filter = listOf("N01"))
          .body("content.size()", equalTo(3))
          .body("probationAreaAggregations.size()", CoreMatchers.equalTo(3))
          .body("probationAreaAggregations[0].code", CoreMatchers.equalTo("N01"))
          .body("probationAreaAggregations[0].count", CoreMatchers.equalTo(3))
          .body("probationAreaAggregations[1].code", CoreMatchers.equalTo("N07"))
          .body("probationAreaAggregations[1].count", CoreMatchers.equalTo(2))
          .body("probationAreaAggregations[2].code", CoreMatchers.equalTo("N02"))
          .body("probationAreaAggregations[2].count", CoreMatchers.equalTo(1))
    }
  }

  @Nested
  @TestInstance(PER_CLASS)
  inner class WordHighlighting {
    @Suppress("unused")
    fun matchAllTerms() = listOf(false, true)

    @BeforeAll
    internal fun loadOffenders() {
      loadOffenders(
          OffenderReplacement(
              crn = "X00001",
              surname = "Smith",
              firstName = "John",
              dateOfBirth = LocalDate.parse("1999-12-22")
          ),
          OffenderReplacement(
              crn = "X00002",
              surname = "John",
              firstName = "Smith"
          ),
          OffenderReplacement(
              crn = "X00003",
              surname = "Jones",
              firstName = "Fred",
              aliases = listOf(
                  AliasReplacement(surname = "Smith", firstName = "John"),
                  AliasReplacement(surname = "SMITH", firstName = "Jim"),
                  AliasReplacement(surname = "John", firstName = "Smith")
              )
          ),
          OffenderReplacement(
              surname = "Jones",
              crn = "X00004",
              streetName = "28 Smith Street"
          )
      )
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `will return highlights for each offender for the word matched`(matchAllTerms: Boolean) {
      doSearch("smith", matchAllTerms)
          .body("content.size()", equalTo(4))
          .body("content.find { it.otherIds.crn == \"X00001\" }.highlight.surname.size()", equalTo(1))
          .body("content.find { it.otherIds.crn == \"X00001\" }.highlight.surname[0]", equalTo("Smith"))

          .body("content.find { it.otherIds.crn == \"X00002\" }.highlight.firstName.size()", equalTo(1))
          .body("content.find { it.otherIds.crn == \"X00002\" }.highlight.firstName[0]", equalTo("Smith"))

          .body("content.find { it.otherIds.crn == \"X00003\" }.highlight.\"offenderAliases.firstName\".size()", equalTo(1))
          .body("content.find { it.otherIds.crn == \"X00003\" }.highlight.\"offenderAliases.firstName\"[0]", equalTo("Smith"))
          .body("content.find { it.otherIds.crn == \"X00003\" }.highlight.\"offenderAliases.surname\".size()", equalTo(2))
          .body("content.find { it.otherIds.crn == \"X00003\" }.highlight.\"offenderAliases.surname\"[0]", equalTo("Smith"))
          .body("content.find { it.otherIds.crn == \"X00003\" }.highlight.\"offenderAliases.surname\"[1]", equalTo("SMITH"))

          .body("content.find { it.otherIds.crn == \"X00004\" }.highlight.\"contactDetails.addresses.streetName\".size()", equalTo(1))
          .body("content.find { it.otherIds.crn == \"X00004\" }.highlight.\"contactDetails.addresses.streetName\"[0]", equalTo("28 Smith Street"))
    }

    @ParameterizedTest
    @MethodSource("matchAllTerms")
    internal fun `will return highlights for date of birth regardless of format searched for`(matchAllTerms: Boolean) {
      doSearch("1999-12-22", matchAllTerms)
          .body("content.size()", equalTo(1))
          .body("content.find { it.otherIds.crn == \"X00001\" }.highlight.dateOfBirth.size()", equalTo(1))
          .body("content.find { it.otherIds.crn == \"X00001\" }.highlight.dateOfBirth[0]", equalTo("1999-12-22"))

      doSearch("22-12-1999", matchAllTerms)
          .body("content.size()", equalTo(1))
          .body("content.find { it.otherIds.crn == \"X00001\" }.highlight.dateOfBirth.size()", equalTo(1))
          .body("content.find { it.otherIds.crn == \"X00001\" }.highlight.dateOfBirth[0]", equalTo("1999-12-22"))
    }
  }

  @Nested
  @TestInstance(PER_CLASS)
  inner class ExclusionAndInclusions {
    @BeforeAll
    internal fun loadOffenders() {
      loadOffenders(
          OffenderReplacement(
              crn = "X00001",
              surname = "Smith",
              firstName = "John",
              currentRestriction = false,
              currentExclusion = false
          ),
          OffenderReplacement(
              crn = "X00002",
              surname = "Smith",
              firstName = "John",
              currentRestriction = true,
              currentExclusion = false
          ),
          OffenderReplacement(
              crn = "X00003",
              surname = "Smith",
              firstName = "John",
              currentRestriction = true,
              currentExclusion = false
          ),
          OffenderReplacement(
              crn = "X00004",
              surname = "Smith",
              firstName = "John",
              currentRestriction = false,
              currentExclusion = true
          ),
          OffenderReplacement(
              crn = "X00005",
              surname = "Smith",
              firstName = "John",
              currentRestriction = false,
              currentExclusion = true
          ),
      )
      CommunityApiExtension.communityApi.stubUserAccess(crn = "X00002", response = """
        {
            "userRestricted": true,
            "userExcluded": false
        }
      """.trimIndent())
      CommunityApiExtension.communityApi.stubUserAccess(crn = "X00003", response = """
        {
            "userRestricted": false,
            "userExcluded": false
        }
      """.trimIndent())
      CommunityApiExtension.communityApi.stubUserAccess(crn = "X00004", response = """
        {
            "userRestricted": false,
            "userExcluded": true
        }
      """.trimIndent())
      CommunityApiExtension.communityApi.stubUserAccess(crn = "X00005", response = """
        {
            "userRestricted": false,
            "userExcluded": false
        }
      """.trimIndent())
    }

    @Nested
    inner class WithClientHonoringBothList {
      @Nested
      inner class WithDeliusUserPresent {
        val token = jwtAuthenticationHelper.createCommunityJwtWithScopes(ClientUser(clientId = "new-tech", subject = "maryblacknps", username = "maryblacknps", authSource = "delius"), "read")

        @Test
        internal fun `can view all details for offenders neither with exclusion nor inclusion lists`() {
          doSearch("X00001", token = token)
              .body("content[0].otherIds.crn", equalTo("X00001"))
              .body("content[0].firstName", equalTo("John"))
              .body("content[0].surname", equalTo("Smith"))
              .body("content[0].accessDenied", nullValue())
        }
        @Test
        internal fun `will be denied access to offenders that have inclusion lists that current user is not on`() {
          doSearch("X00002", token = token)
              .body("content[0].otherIds.crn", equalTo("X00002"))
              .body("content[0].firstName", nullValue())
              .body("content[0].surname", nullValue())
              .body("content[0].accessDenied", equalTo(true))
        }
        @Test
        internal fun `will be allowed access to offenders that have inclusion lists that current user is on`() {
          doSearch("X00003", token = token)
              .body("content[0].otherIds.crn", equalTo("X00003"))
              .body("content[0].firstName", equalTo("John"))
              .body("content[0].surname", equalTo("Smith"))
              .body("content[0].accessDenied", nullValue())
        }
        @Test
        internal fun `will be denied access to offenders that have exclusion lists that current user is on`() {
          doSearch("X00004", token = token)
              .body("content[0].otherIds.crn", equalTo("X00004"))
              .body("content[0].firstName", nullValue())
              .body("content[0].surname", nullValue())
              .body("content[0].accessDenied", equalTo(true))
        }
        @Test
        internal fun `will be allowed access to offenders that have exclusion lists that current user is not on`() {
          doSearch("X00005", token = token)
              .body("content[0].otherIds.crn", equalTo("X00005"))
              .body("content[0].firstName", equalTo("John"))
              .body("content[0].surname", equalTo("Smith"))
              .body("content[0].accessDenied", nullValue())
        }

        @Test
        internal fun `will pass jwt token through to community-api when checking access restrictions`() {
          doSearch("X00004", token = token)
              .body("content[0].otherIds.crn", equalTo("X00004"))
              .body("content[0].firstName", nullValue())
              .body("content[0].surname", nullValue())
              .body("content[0].accessDenied", equalTo(true))

          CommunityApiExtension.communityApi.verify(WireMock.getRequestedFor(WireMock.urlEqualTo("/secure/offenders/crn/X00004/userAccess"))
              .withHeader("Authorization", WireMock.equalTo("Bearer $token")))
        }
      }

      @Nested
      inner class WithDeliusUserNotPresent {
        val token = jwtAuthenticationHelper.createCommunityJwtWithScopes(ClientUser(clientId = "new-tech", subject = "maryblackdps", username = "maryblackdps", authSource = "nomis"), "read")

        @Test
        internal fun `can view all details for offenders neither with exclusion nor inclusion lists`() {
          doSearch("X00001", token = token)
              .body("content[0].otherIds.crn", equalTo("X00001"))
              .body("content[0].firstName", equalTo("John"))
              .body("content[0].surname", equalTo("Smith"))
              .body("content[0].accessDenied", nullValue())
        }
        @Test
        internal fun `will be denied access to offenders that have inclusion lists that can not be checked`() {
          doSearch("X00002", token = token)
              .body("content[0].otherIds.crn", equalTo("X00002"))
              .body("content[0].firstName", nullValue())
              .body("content[0].surname", nullValue())
              .body("content[0].accessDenied", equalTo(true))
          doSearch("X00003", token = token)
              .body("content[0].otherIds.crn", equalTo("X00003"))
              .body("content[0].firstName", nullValue())
              .body("content[0].surname", nullValue())
              .body("content[0].accessDenied", equalTo(true))
        }
        @Test
        internal fun `will be denied access to offenders that have exclusion lists that can not be checked`() {
          doSearch("X00004", token = token)
              .body("content[0].otherIds.crn", equalTo("X00004"))
              .body("content[0].firstName", nullValue())
              .body("content[0].surname", nullValue())
              .body("content[0].accessDenied", equalTo(true))
          doSearch("X00005", token = token)
              .body("content[0].otherIds.crn", equalTo("X00005"))
              .body("content[0].firstName", nullValue())
              .body("content[0].surname", nullValue())
              .body("content[0].accessDenied", equalTo(true))
        }
      }
    }
    @Nested
    inner class WithClientHonoringInclusionListOnly {
      @Nested
      inner class WithDeliusUserPresent {
        val token = jwtAuthenticationHelper.createCommunityJwtWithScopes(ClientUser(clientId = "new-tech", subject = "maryblacknps", username = "maryblacknps", authSource = "delius"), "read", "ignore_delius_exclusions_always")

        @Test
        internal fun `can view all details for offenders neither with exclusion nor inclusion lists`() {
          doSearch("X00001", token = token)
              .body("content[0].otherIds.crn", equalTo("X00001"))
              .body("content[0].firstName", equalTo("John"))
              .body("content[0].surname", equalTo("Smith"))
              .body("content[0].accessDenied", nullValue())
        }
        @Test
        internal fun `will be denied access to offenders that have inclusion lists that current user is not on`() {
          doSearch("X00002", token = token)
              .body("content[0].otherIds.crn", equalTo("X00002"))
              .body("content[0].firstName", nullValue())
              .body("content[0].surname", nullValue())
              .body("content[0].accessDenied", equalTo(true))
        }
        @Test
        internal fun `will be allowed access to offenders that have inclusion lists that current user is on`() {
          doSearch("X00003", token = token)
              .body("content[0].otherIds.crn", equalTo("X00003"))
              .body("content[0].firstName", equalTo("John"))
              .body("content[0].surname", equalTo("Smith"))
              .body("content[0].accessDenied", nullValue())
        }
        @Test
        internal fun `will be allowed access to offenders that have exclusion lists even though current user is on list`() {
          doSearch("X00004", token = token)
              .body("content[0].otherIds.crn", equalTo("X00004"))
              .body("content[0].firstName", equalTo("John"))
              .body("content[0].surname", equalTo("Smith"))
              .body("content[0].accessDenied", nullValue())
        }
        @Test
        internal fun `will be allowed access to offenders that have exclusion lists that current user is not on`() {
          doSearch("X00005", token = token)
              .body("content[0].otherIds.crn", equalTo("X00005"))
              .body("content[0].firstName", equalTo("John"))
              .body("content[0].surname", equalTo("Smith"))
              .body("content[0].accessDenied", nullValue())
        }
      }

      @Nested
      inner class WithDeliusUserNotPresent {
        val token = jwtAuthenticationHelper.createCommunityJwtWithScopes(ClientUser(clientId = "new-tech", subject = "maryblackdps", username = "maryblackdps", authSource = "nomis"), "read", "ignore_delius_exclusions_always")

        @Test
        internal fun `can view all details for offenders neither with exclusion nor inclusion lists`() {
          doSearch("X00001", token = token)
              .body("content[0].otherIds.crn", equalTo("X00001"))
              .body("content[0].firstName", equalTo("John"))
              .body("content[0].surname", equalTo("Smith"))
              .body("content[0].accessDenied", nullValue())
        }
        @Test
        internal fun `will be denied access to offenders that have inclusion lists that can not be checked`() {
          doSearch("X00002", token = token)
              .body("content[0].otherIds.crn", equalTo("X00002"))
              .body("content[0].firstName", nullValue())
              .body("content[0].surname", nullValue())
              .body("content[0].accessDenied", equalTo(true))
          doSearch("X00003", token = token)
              .body("content[0].otherIds.crn", equalTo("X00003"))
              .body("content[0].firstName", nullValue())
              .body("content[0].surname", nullValue())
              .body("content[0].accessDenied", equalTo(true))
        }

        @Test
        internal fun `will be allowed access to offenders that have exclusion lists even though they can not be checked`() {
          doSearch("X00004", token = token)
              .body("content[0].otherIds.crn", equalTo("X00004"))
              .body("content[0].firstName", equalTo("John"))
              .body("content[0].surname", equalTo("Smith"))
              .body("content[0].accessDenied", nullValue())
          doSearch("X00005", token = token)
              .body("content[0].otherIds.crn", equalTo("X00005"))
              .body("content[0].firstName", equalTo("John"))
              .body("content[0].surname", equalTo("Smith"))
              .body("content[0].accessDenied", nullValue())
        }
      }
    }
    @Nested
    inner class WithClientHonoringExclusionListOnly {
      @Nested
      inner class WithDeliusUserPresent {
        val token = jwtAuthenticationHelper.createCommunityJwtWithScopes(ClientUser(clientId = "new-tech", subject = "maryblacknps", username = "maryblacknps", authSource = "delius"), "read", "ignore_delius_inclusions_always")

        @Test
        internal fun `can view all details for offenders neither with exclusion nor inclusion lists`() {
          doSearch("X00001", token = token)
              .body("content[0].otherIds.crn", equalTo("X00001"))
              .body("content[0].firstName", equalTo("John"))
              .body("content[0].surname", equalTo("Smith"))
              .body("content[0].accessDenied", nullValue())
        }
        @Test
        internal fun `will be allowed access to offenders that have inclusion lists event though current user is not on list`() {
          doSearch("X00002", token = token)
              .body("content[0].otherIds.crn", equalTo("X00002"))
              .body("content[0].firstName", equalTo("John"))
              .body("content[0].surname", equalTo("Smith"))
              .body("content[0].accessDenied", nullValue())
        }
        @Test
        internal fun `will be allowed access to offenders that have inclusion lists that current user is on`() {
          doSearch("X00003", token = token)
              .body("content[0].otherIds.crn", equalTo("X00003"))
              .body("content[0].firstName", equalTo("John"))
              .body("content[0].surname", equalTo("Smith"))
              .body("content[0].accessDenied", nullValue())
        }
        @Test
        internal fun `will be denied access to offenders that have exclusion lists that current user is on`() {
          doSearch("X00004", token = token)
              .body("content[0].otherIds.crn", equalTo("X00004"))
              .body("content[0].firstName", nullValue())
              .body("content[0].surname", nullValue())
              .body("content[0].accessDenied", equalTo(true))
        }
        @Test
        internal fun `will be allowed access to offenders that have exclusion lists that current user is not on`() {
          doSearch("X00005", token = token)
              .body("content[0].otherIds.crn", equalTo("X00005"))
              .body("content[0].firstName", equalTo("John"))
              .body("content[0].surname", equalTo("Smith"))
              .body("content[0].accessDenied", nullValue())
        }
      }

      @Nested
      inner class WithDeliusUserNotPresent {
        val token = jwtAuthenticationHelper.createCommunityJwtWithScopes(ClientUser(clientId = "new-tech", subject = "maryblackdps", username = "maryblackdps", authSource = "nomis"), "read", "ignore_delius_inclusions_always")

        @Test
        internal fun `can view all details for offenders neither with exclusion nor inclusion lists`() {
          doSearch("X00001", token = token)
              .body("content[0].otherIds.crn", equalTo("X00001"))
              .body("content[0].firstName", equalTo("John"))
              .body("content[0].surname", equalTo("Smith"))
              .body("content[0].accessDenied", nullValue())
        }
        @Test
        internal fun `will be allowed access to offenders that have inclusion lists even though they can not be checked`() {
          doSearch("X00002", token = token)
              .body("content[0].otherIds.crn", equalTo("X00002"))
              .body("content[0].firstName", equalTo("John"))
              .body("content[0].surname", equalTo("Smith"))
              .body("content[0].accessDenied", nullValue())
          doSearch("X00003", token = token)
              .body("content[0].otherIds.crn", equalTo("X00003"))
              .body("content[0].firstName", equalTo("John"))
              .body("content[0].surname", equalTo("Smith"))
              .body("content[0].accessDenied", nullValue())
        }

        @Test
        internal fun `will be denied access to offenders that have exclusion lists that can not be checked`() {
          doSearch("X00004", token = token)
              .body("content[0].otherIds.crn", equalTo("X00004"))
              .body("content[0].firstName", nullValue())
              .body("content[0].surname", nullValue())
              .body("content[0].accessDenied", equalTo(true))
          doSearch("X00005", token = token)
              .body("content[0].otherIds.crn", equalTo("X00005"))
              .body("content[0].firstName", nullValue())
              .body("content[0].surname", nullValue())
              .body("content[0].accessDenied", equalTo(true))
        }
      }
    }
  }

  private fun hasSingleMatch(phrase: String, @Suppress("SameParameterValue") expectedCrn: String, matchAllTerms: Boolean = false) {
    doSearch(phrase = phrase, matchAllTerms = matchAllTerms)
        .body("totalElements", equalTo(1))
        .body("content[0].otherIds.crn", equalTo(expectedCrn))
  }

  private fun hasMatches(phrase: String, matchAllTerms: Boolean = false, expectedCrns: List<String>, filter: List<String> = listOf()) {
    val response = doSearch(phrase = phrase, matchAllTerms = matchAllTerms, filter = filter)

    expectedCrns.forEach {
      response
          .body("content.find { it.otherIds.crn == \"$it\" }.otherIds.crn", equalTo(it))
    }

    response
        .body("totalElements", equalTo(expectedCrns.size))
  }

  private fun hasNoMatch(phrase: String, matchAllTerms: Boolean = false) {
    doSearch(phrase = phrase, matchAllTerms = matchAllTerms)
        .body("totalElements", equalTo(0))
  }

  private fun doSearch(phrase: String, matchAllTerms: Boolean = false, size: Int? = null, page: Int? = null, filter: List<String> = listOf(), token: String = jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY")): ValidatableResponse {
    val searchPhraseFilter = SearchPhraseFilter(phrase = phrase, matchAllTerms = matchAllTerms, probationAreasFilter = filter)
    val request = RestAssured.given()
        .auth()
        .oauth2(token)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body(searchPhraseFilter)
        .apply {
          size?.also { this.queryParam("size", it) }
          page?.also { this.queryParam("page", it) }
        }

    return request
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
          },
          currentExclusion = it.currentExclusion,
          currentRestriction = it.currentRestriction
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
    val offenderManagers: List<OffenderManagerReplacement> = listOf(OffenderManagerReplacement()),
    val currentRestriction: Boolean = false,
    val currentExclusion: Boolean = false
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
