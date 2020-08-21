package uk.gov.justice.hmpps.offendersearch.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.restassured.RestAssured
import io.restassured.config.ObjectMapperConfig
import io.restassured.config.RestAssuredConfig
import org.assertj.core.api.Assertions.assertThat
import org.elasticsearch.common.text.Text
import org.elasticsearch.search.suggest.SortBy.SCORE
import org.elasticsearch.search.suggest.Suggest
import org.elasticsearch.search.suggest.term.TermSuggestion
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.hmpps.offendersearch.dto.IDs
import uk.gov.justice.hmpps.offendersearch.dto.OffenderDetail
import uk.gov.justice.hmpps.offendersearch.dto.ProbationAreaAggregation
import uk.gov.justice.hmpps.offendersearch.dto.SearchPhraseResults
import uk.gov.justice.hmpps.offendersearch.services.SearchService
import uk.gov.justice.hmpps.offendersearch.util.JwtAuthenticationHelper
import java.lang.reflect.Type
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(SpringExtension::class)
class OffenderSearchControllerPhraseTest {
  @LocalServerPort
  var port = 0

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Autowired
  private lateinit var jwtAuthenticationHelper: JwtAuthenticationHelper

  @MockBean
  private lateinit var searchService: SearchService

  @BeforeEach
  internal fun setUp() {
    RestAssured.port = port
    RestAssured.config = RestAssuredConfig.config().objectMapperConfig(
        ObjectMapperConfig().jackson2ObjectMapperFactory { _: Type?, _: String? -> objectMapper })
    whenever(searchService.performSearch(any(), any())).thenReturn(SearchPhraseResults(content = listOf(), total = 0, pageable = PageRequest.of(0, 10), probationAreaAggregations = listOf(), suggestions = null))
  }

  @Test
  internal fun `must supply authentication`() {
    RestAssured.given()
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("""{
          | "phrase": "john smith 19/7/1965"
          |}""".trimMargin())
        .post("/phrase")
        .then()
        .statusCode(401)
  }

  @Test
  internal fun `ROLE_COMMUNITY is required`() {
    RestAssured.given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_BANANAS"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("""{
          | "phrase": "john smith 19/7/1965"
          |}""".trimMargin())
        .post("/phrase")
        .then()
        .statusCode(403)
  }

  @Test
  internal fun `phrase is mandatory`() {
    RestAssured.given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("{}")
        .post("/phrase")
        .then()
        .statusCode(400)
        .body("developerMessage", containsString("phrase must be supplied"))
  }

  @Test
  internal fun `will return 200 when a valid search is performed`() {
    RestAssured.given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("""{
          | "phrase": "john smith 19/7/1965"
          |}""".trimMargin())
        .post("/phrase")
        .then()
        .statusCode(200)
  }

  @Test
  internal fun `will default page to zero`() {
    RestAssured.given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("""{
          | "phrase": "john smith 19/7/1965"
          |}""".trimMargin())
        .post("/phrase")
        .then()
        .statusCode(200)

    verify(searchService).performSearch(any(), check {
      assertThat(it.pageNumber).isEqualTo(0)
    })
  }

  @Test
  internal fun `can supply page number`() {
    RestAssured.given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .queryParam("page", 28)
        .body("""{
          | "phrase": "john smith 19/7/1965"
          |}""".trimMargin())
        .post("/phrase")
        .then()
        .statusCode(200)

    verify(searchService).performSearch(any(), check {
      assertThat(it.pageNumber).isEqualTo(28)
    })
  }

  @Test
  internal fun `will default page size to ten`() {
    RestAssured.given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("""{
          | "phrase": "john smith 19/7/1965"
          |}""".trimMargin())
        .post("/phrase")
        .then()
        .statusCode(200)

    verify(searchService).performSearch(any(), check {
      assertThat(it.pageSize).isEqualTo(10)
    })
  }

  @Test
  internal fun `can supply page size`() {
    RestAssured.given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .queryParam("size", 99)
        .body("""{
          | "phrase": "john smith 19/7/1965"
          |}""".trimMargin())
        .post("/phrase")
        .then()
        .statusCode(200)

    verify(searchService).performSearch(any(), check {
      assertThat(it.pageSize).isEqualTo(99)
    })
  }

  @Test
  internal fun `will default to an OR search`() {
    RestAssured.given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("""{
          | "phrase": "john smith 19/7/1965"
          |}""".trimMargin())
        .post("/phrase")
        .then()
        .statusCode(200)

    verify(searchService).performSearch(check {
      assertThat(it.matchAllTerms).isFalse()
    }, any())
  }

  @Test
  internal fun `can supply search type`() {
    RestAssured.given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("""{
          | "phrase": "john smith 19/7/1965",
          | "matchAllTerms": true
          |}""".trimMargin())
        .post("/phrase")
        .then()
        .statusCode(200)

    verify(searchService).performSearch(check {
      assertThat(it.matchAllTerms).isTrue()
    }, any())
  }

  @Test
  internal fun `will by default not include probation area filter`() {
    RestAssured.given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("""{
          | "phrase": "john smith 19/7/1965"
          |}""".trimMargin())
        .post("/phrase")
        .then()
        .statusCode(200)

    verify(searchService).performSearch(check {
      assertThat(it.probationAreasFilter).isEmpty()
    }, any())
  }

  @Test
  internal fun `can supply a probation area filter`() {
    RestAssured.given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("""{
          | "phrase": "john smith 19/7/1965",
          | "probationAreasFilter": [
          |   "N01",
          |   "N02"
          | ]
          |}""".trimMargin())
        .post("/phrase")
        .then()
        .statusCode(200)

    verify(searchService).performSearch(check {
      assertThat(it.probationAreasFilter).containsExactly("N01", "N02")
    }, any())
  }

  @Test
  internal fun `will return results when matched`() {
    whenever(searchService.performSearch(any(), any())).thenReturn(SearchPhraseResults(
        content = listOf(OffenderDetail(offenderId = 99, firstName = "John", surname = "Smith", dateOfBirth = LocalDate.parse("1965-07-19"), otherIds = IDs(crn = "X123456"))),
        total = 1,
        probationAreaAggregations = listOf(),
        suggestions = null,
        pageable = PageRequest.of(0, 10)
    ))

    RestAssured.given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("""{
          | "phrase": "john smith 19/7/1965"
          |}""".trimMargin())
        .post("/phrase")
        .then()
        .statusCode(200)
        .body("totalElements", equalTo(1))
        .body("content.size()", equalTo(1))
        .body("content[0].offenderId", equalTo(99))
        .body("content[0].surname", equalTo("Smith"))
        .body("content[0].dateOfBirth", equalTo("1965-07-19"))
        .body("content[0].otherIds.crn", equalTo("X123456"))
  }

  @Test
  internal fun `will return probation area aggregations`() {
    whenever(searchService.performSearch(any(), any())).thenReturn(SearchPhraseResults(
        content = listOf(OffenderDetail(offenderId = 99, firstName = "John", surname = "Smith", dateOfBirth = LocalDate.parse("1965-07-19"), otherIds = IDs(crn = "X123456"))),
        total = 1,
        probationAreaAggregations = listOf(
            ProbationAreaAggregation(
                code = "N01",
                count = 8
            ),
            ProbationAreaAggregation(
                code = "N02",
                count = 2
            )
        ),
        suggestions = null,
        pageable = PageRequest.of(0, 10)
    ))

    RestAssured.given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("""{
          | "phrase": "john smith 19/7/1965"
          |}""".trimMargin())
        .post("/phrase")
        .then()
        .statusCode(200)
        .body("totalElements", equalTo(1))
        .body("probationAreaAggregations.size()", equalTo(2))
        .body("probationAreaAggregations[0].code", equalTo("N01"))
        .body("probationAreaAggregations[1].code", equalTo("N02"))
  }

  @Test
  internal fun `will return suggestions`() {
    val suggestion: TermSuggestion = TermSuggestion("firstName", 5, SCORE).apply {
      this.addTerm(TermSuggestion.Entry(Text("smyth"), 0, 5).apply {
        this.addOption(TermSuggestion.Entry.Option(Text("smith"), 1, 0.8f))
        this.addOption(TermSuggestion.Entry.Option(Text("smita"), 1, 0.8f))
        this.addOption(TermSuggestion.Entry.Option(Text("sumith"), 1, 0.8f))
      })
    }
    whenever(searchService.performSearch(any(), any())).thenReturn(SearchPhraseResults(
        content = listOf(OffenderDetail(offenderId = 99, firstName = "John", surname = "Smith", dateOfBirth = LocalDate.parse("1965-07-19"), otherIds = IDs(crn = "X123456"))),
        total = 1,
        probationAreaAggregations = listOf(),
        suggestions = Suggest(listOf(suggestion)),
        pageable = PageRequest.of(0, 10)
    ))


    RestAssured.given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("""{
          | "phrase": "john smith 19/7/1965"
          |}""".trimMargin())
        .post("/phrase")
        .then()
        .statusCode(200)
        .body("suggestions.suggest.firstName[0].options.size()", equalTo(3))
  }
}