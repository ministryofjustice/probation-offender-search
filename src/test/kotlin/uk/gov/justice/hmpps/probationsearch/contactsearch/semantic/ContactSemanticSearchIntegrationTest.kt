package uk.gov.justice.hmpps.probationsearch.contactsearch.semantic

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

  @Value("\${local.server.port}")
  internal val port: Int = 0

  internal val deliusApiMock = DeliusApiExtension.Companion.deliusApi

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
    val crn = "Z123456"
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
        )
      ),
      anyMap(),
    )
  }

  @Test
  fun `highlights the most semantically similar chunk`() {
    val crn = "Z123456"
    val query = "criminology"
    val results = RestAssured.given().`when`().search(ContactSearchRequest(crn, query)).then().results()

    assertThat(results.size).isEqualTo(1)
    assertThat(results.results[0].highlights["notes"]?.first()).isEqualTo(
      """
                He was still, as <em>ever, deeply attracted by the study of crime,
                and occupied his immense faculties and extraordinary powers of
                observation in following out those clues, and clearing up those
                mysteries which had been abandoned</em> as hopeless by the official police.
            """.trimIndent()
    )
  }

  @Test
  fun `highlight is wrapped in ellipsis when fragment size would be too large`() {
    val crn = "Z123456"
    val query = "addiction"
    val results = RestAssured.given().`when`().search(ContactSearchRequest(crn, query)).then().results()

    assertThat(results.size).isEqualTo(1)
    assertThat(results.results[0].highlights["notes"]?.first()).isEqualTo(
      """
                ...<em>and alternating from week to week between cocaine and ambition,
                the drowsiness of the drug, and the fierce energy of his own keen
                nature. He was still, as ever, deeply attracted by</em>...
            """.trimIndent()
    )
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