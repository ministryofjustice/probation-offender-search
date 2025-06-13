package uk.gov.justice.hmpps.probationsearch.contactsearch.semantic

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import io.restassured.RestAssured
import io.restassured.response.ValidatableResponse
import io.restassured.specification.RequestSpecification
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.justice.hmpps.probationsearch.contactsearch.OpenSearchSetup
import uk.gov.justice.hmpps.probationsearch.contactsearch.model.ContactSearchRequest
import uk.gov.justice.hmpps.probationsearch.contactsearch.model.ContactSearchResponse
import uk.gov.justice.hmpps.probationsearch.dto.ContactJson
import uk.gov.justice.hmpps.probationsearch.services.FeatureFlags
import uk.gov.justice.hmpps.probationsearch.util.JwtAuthenticationHelper
import uk.gov.justice.hmpps.probationsearch.wiremock.DeliusApiExtension

@ExtendWith(DeliusApiExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["test"])
class ContactSemanticSearchIntegrationTest {
  @MockitoBean
  private lateinit var featureFlags: FeatureFlags

  @Autowired
  internal lateinit var jwtAuthenticationHelper: JwtAuthenticationHelper

  @Autowired
  internal lateinit var openSearchSetup: OpenSearchSetup

  @Autowired
  internal lateinit var objectMapper: ObjectMapper

  @Value("\${local.server.port}")
  internal val port: Int = 0

  internal val deliusApiMock = DeliusApiExtension.deliusApi

  @MockitoBean
  internal lateinit var telemetry: TelemetryClient

  @BeforeEach
  internal fun before() {
    RestAssured.port = port
    deliusApiMock.stubDeliusAuditSuccess()
    openSearchSetup.setup()
    whenever(featureFlags.enabled("semantic-contact-search")).thenReturn(true)
  }

  @Test
  fun `logs a custom event to telemetry`() {
    val crn = "S123456"
    val query = "test test"
    RestAssured.given().`when`().search(ContactSearchRequest(crn, query)).then().results()

    verify(telemetry).trackEvent(
      eq("SemanticSearchCompleted"),
      eq(
        mapOf(
          "crn" to crn,
          "query" to "9",
          "resultCount" to "1",
          "queryTermCount" to "2",
          "page" to "0",
          "resultCountForPage" to "1",
          "semanticOnlyResultCountForPage" to "1",
        ),
      ),
      anyMap(),
    )
  }

  @Test
  fun `highlights the most semantically similar chunk`() {
    val crn = "S123456"
    val query = "criminology"
    val results = RestAssured.given().`when`().search(ContactSearchRequest(crn, query)).then().results()

    assertThat(results.size).isEqualTo(1)
    assertThat(results.results[0].highlights["notes"]?.first()).isEqualTo(
      """
          He was still, as <em>ever, deeply attracted by the study of crime,
          and occupied his immense faculties and extraordinary powers of
          observation in following out those clues, and clearing up those
          mysteries which had been abandoned</em> as hopeless by the official police.
      """.trimIndent(),
    )
  }

  @Test
  fun `highlight is wrapped in ellipsis when fragment size would be too large`() {
    val crn = "S123456"
    val query = "addiction"
    val results = RestAssured.given().`when`().search(ContactSearchRequest(crn, query)).then().results()

    assertThat(results.size).isEqualTo(1)
    assertThat(results.results[0].highlights["notes"]?.first()).isEqualTo(
      """
          ...<em>and alternating from week to week between cocaine and ambition,
          the drowsiness of the drug, and the fierce energy of his own keen
          nature. He was still, as ever, deeply attracted by</em>...
      """.trimIndent(),
    )
  }

  @Test
  fun `loads data on demand if CRN not present in index`() {
    val crn = "Z123456"
    deliusApiMock.stubGetContacts(
      crn,
      objectMapper.writeValueAsString(
        listOf(
          ContactJson(
            contactId = 9000001,
            version = 1,
            json = """{"crn": "Z123456", "id": 9000001, "notes": "This is a new contact for Z123456", "typeCode": "test", "typeDescription": "test", "date": "2000-01-01T00:00:00", "lastUpdatedDateTime": "2000-01-01T00:00:00"}""",
          ),
          ContactJson(
            contactId = 9000002,
            version = 1,
            json = """{"crn": "Z123456", "id": 9000002, "notes": "Another new contact for Z123456", "typeCode": "test", "typeDescription": "test", "date": "2000-01-01T00:00:00", "lastUpdatedDateTime": "2000-01-01T00:00:00"}""",
          ),
        ),
      ),
    )

    val query = "new contact"
    val results = RestAssured.given().`when`().search(ContactSearchRequest(crn, query)).then().results()

    assertThat(results.size).isEqualTo(2)
    assertThat(results.results.map { it.id }).contains(9000001, 9000002)
    verify(telemetry).trackEvent(eq("OnDemandDataLoad"), eq(mapOf("crn" to crn)), anyMap())
  }

  @Test
  fun `returns all contacts for empty query`() {
    val crn = "N123456"
    val query = ""
    val results = RestAssured.given().`when`().search(ContactSearchRequest(crn, query)).then().results()

    assertThat(results.totalResults).isEqualTo(5)
    assertThat(results.results.size).isEqualTo(5)
    results.results.forEach { assertThat(it.crn).isEqualTo(crn) }
  }

  @Test
  fun `pagination works correctly for search results`() {
    val crn = "N123456"
    val query = "" // Use empty query to get all results for pagination
    val pageSize = 2
    val page0Results = RestAssured.given()
      .`when`().search(ContactSearchRequest(crn, query), mapOf("size" to pageSize, "page" to 0))
      .then().results()
    val page1Results = RestAssured.given()
      .`when`().search(ContactSearchRequest(crn, query), mapOf("size" to pageSize, "page" to 1))
      .then().results()

    assertThat(page0Results.totalResults).isEqualTo(5)
    assertThat(page0Results.totalPages).isEqualTo(3)
    assertThat(page0Results.results.size).isEqualTo(2)
    assertThat(page0Results.page).isEqualTo(0)

    assertThat(page1Results.totalResults).isEqualTo(5)
    assertThat(page1Results.totalPages).isEqualTo(3)
    assertThat(page1Results.results.size).isEqualTo(2)
    assertThat(page1Results.page).isEqualTo(1)

    assertThat(page0Results.results).isNotEqualTo(page1Results.results)
  }

  @Test
  fun `search by typeDescription returns correct keyword results`() {
    val crn = "H123456"
    val query = "type"
    val results = RestAssured.given().`when`().search(ContactSearchRequest(crn, query)).then().results()

    assertThat(results.size).isEqualTo(1)
    assertThat(results.results[0].typeDescription).isEqualTo("Matches should be highlighted in type")
  }

  @Test
  fun `search by outcomeDescription returns correct keyword results`() {
    val crn = "H123456"
    val query = "outcome"
    val results = RestAssured.given().`when`().search(ContactSearchRequest(crn, query)).then().results()

    assertThat(results.size).isEqualTo(1)
    assertThat(results.results[0].outcomeDescription).isEqualTo("Matches were highlighted in outcome")
  }

  @Test
  fun `search with matchAllTerms true returns only results matching all terms`() {
    val crn = "S123456"
    val query = "cocaine ambition" // Both terms are in the notes
    val request = ContactSearchRequest(crn, query, matchAllTerms = true)
    val results = RestAssured.given().`when`().search(request).then().results()

    assertThat(results.size).isEqualTo(1)
    assertThat(results.results[0].highlights["notes"]?.get(0)).contains("cocaine")
    assertThat(results.results[0].highlights["notes"]?.get(1)).contains("ambition")
  }

  @Test
  fun `search with matchAllTerms false returns results matching any term`() {
    val crn = "S123456"
    val query = "non_existent_word ambition" // Only 'ambition' is in the notes
    val request = ContactSearchRequest(crn, query, matchAllTerms = false)
    val results = RestAssured.given().`when`().search(request).then().results()

    assertThat(results.size).isEqualTo(1)
    assertThat(results.results[0].highlights["notes"]?.first()).contains("ambition")
  }

  private fun RequestSpecification.authorised(): RequestSpecification =
    this.auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_PROBATION_CONTACT_SEARCH"))
      .contentType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE)

  private fun RequestSpecification.search(csr: ContactSearchRequest, queryParams: Map<String, Any> = mapOf()) =
    this.authorised().body(csr).queryParams(queryParams).post("/search/contacts")

  private fun ValidatableResponse.results() =
    this.statusCode(200)
      .extract().body()
      .`as`(ContactSearchResponse::class.java)
}
