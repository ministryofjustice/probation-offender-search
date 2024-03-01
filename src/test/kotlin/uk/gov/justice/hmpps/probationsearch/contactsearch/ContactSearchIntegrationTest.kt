package uk.gov.justice.hmpps.probationsearch.contactsearch

import io.restassured.RestAssured
import io.restassured.response.ValidatableResponse
import io.restassured.specification.RequestSpecification
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.opensearch.action.admin.indices.alias.Alias
import org.opensearch.action.admin.indices.delete.DeleteIndexRequest
import org.opensearch.client.RequestOptions
import org.opensearch.client.indices.CreateIndexRequest
import org.opensearch.client.indices.GetIndexRequest
import org.opensearch.client.indices.PutIndexTemplateRequest
import org.opensearch.common.xcontent.XContentType
import org.opensearch.data.client.orhlc.OpenSearchRestTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.data.elasticsearch.core.query.Query
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.util.ResourceUtils
import uk.gov.justice.hmpps.probationsearch.contactsearch.ContactGenerator.contacts
import uk.gov.justice.hmpps.probationsearch.util.JwtAuthenticationHelper
import uk.gov.justice.hmpps.sqs.audit.HmppsAuditService

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["test"])
class ContactSearchIntegrationTest {

  @Autowired
  internal lateinit var jwtAuthenticationHelper: JwtAuthenticationHelper

  @Autowired
  internal lateinit var openSearchRestTemplate: OpenSearchRestTemplate

  @Value("\${local.server.port}")
  internal val port: Int = 0

  @MockBean
  internal lateinit var auditService: HmppsAuditService

  @BeforeEach
  internal fun before() {
    RestAssured.port = port
    val indexName = "contact-search-a"
    val aliasName = "contact-search-primary"
    openSearchRestTemplate.execute {
      it.indices().putTemplate(
        PutIndexTemplateRequest("contact-search-template").source(TEMPLATE_JSON, XContentType.JSON),
        RequestOptions.DEFAULT,
      )
    }
    openSearchRestTemplate.execute {
      if (it.indices().exists(GetIndexRequest(indexName), RequestOptions.DEFAULT)) {
        it.indices().delete(DeleteIndexRequest(indexName), RequestOptions.DEFAULT)
      }
      it.indices().create(CreateIndexRequest(indexName).alias(Alias(aliasName)), RequestOptions.DEFAULT)
    }
    openSearchRestTemplate.save(contacts, IndexCoordinates.of(aliasName))
    await untilCallTo {
      openSearchRestTemplate.count(
        Query.findAll(),
        IndexCoordinates.of(aliasName),
      )
    } matches { it == contacts.size.toLong() }
  }

  @Test
  fun `can retrieve paginated results and default sort`() {
    val crn = "T123456"
    val results = RestAssured.given()
      .`when`()
      .search(ContactSearchRequest(crn, "CODE"), mapOf("page" to 0, "size" to 3))
      .then()
      .results()

    assertThat(results.size).isEqualTo(3)
    assertThat(results.totalResults).isEqualTo(4)
    assertThat(results.results.map { it.id }).isEqualTo(
      contacts
        .filter { it.crn == crn && it.typeCode == "CODE" }
        .sortedByDescending { it.lastUpdatedDateTime }
        .map { it.id }
        .take(3),
    )
  }

  @ParameterizedTest
  @MethodSource("crnsForFind")
  fun `results only include matches on query and crn, highlighting match criteria`(crn: String) {
    val results = RestAssured.given()
      .`when`()
      .search(ContactSearchRequest(crn, "FIND_ME"))
      .then()
      .results()

    assertThat(results.size).isEqualTo(1)
    val found = results.results.first()
    assertThat(found.crn).isEqualTo(crn)
    assertThat(found.highlights).containsExactlyInAnyOrderEntriesOf(mapOf("type" to listOf("<em>FIND_ME</em>")))
  }

  @Test
  fun `can sort by date`() {
    val crn = "N123456"
    val results = RestAssured.given()
      .`when`()
      .search(ContactSearchRequest(crn, "CODE"), mapOf("size" to 5, "sort" to "date,desc"))
      .then()
      .results()

    assertThat(results.size).isEqualTo(4)
    assertThat(results.totalResults).isEqualTo(4)
    assertThat(results.results.map { it.id }).isEqualTo(
      contacts
        .filter { it.crn == crn && it.typeCode == "CODE" }
        .sortedByDescending { it.date }
        .map { it.id }
        .take(4),
    )
  }

  @Test
  fun `can sort by CONTACT_DATE`() {
    val crn = "N123456"
    val results = RestAssured.given()
      .`when`()
      .search(ContactSearchRequest(crn, "CODE"), mapOf("size" to 5, "sort" to "CONTACT_DATE,desc"))
      .then()
      .results()

    assertThat(results.size).isEqualTo(4)
    assertThat(results.totalResults).isEqualTo(4)
    assertThat(results.results.map { it.id }).isEqualTo(
      contacts
        .filter { it.crn == crn && it.typeCode == "CODE" }
        .sortedByDescending { it.date }
        .map { it.id }
        .take(4),
    )
  }

  @Test
  fun `can sort by last updated`() {
    val crn = "T123456"
    val results = RestAssured.given()
      .`when`()
      .search(ContactSearchRequest(crn, "CODE"), mapOf("size" to 5, "sort" to "lastUpdated,asc"))
      .then()
      .results()

    assertThat(results.size).isEqualTo(4)
    assertThat(results.totalResults).isEqualTo(4)
    assertThat(results.results.map { it.id }).isEqualTo(
      contacts
        .filter { it.crn == crn && it.typeCode == "CODE" }
        .sortedBy { it.lastUpdatedDateTime }
        .map { it.id }
        .take(4),
    )
  }

  @ParameterizedTest
  @MethodSource("datesForFind")
  fun `can search for date in different formats`(date: String) {
    val crn = "Z123456"
    val results = RestAssured.given()
      .`when`()
      .search(ContactSearchRequest(crn, date))
      .then()
      .results()

    assertThat(results.size).isEqualTo(1)
    assertThat(results.results.first().crn).isEqualTo(crn)
  }

  @Test
  fun `matches are highlighted`() {
    val crn = "H123456"
    val results = RestAssured.given()
      .`when`()
      .search(ContactSearchRequest(crn, "highlighted"))
      .then()
      .results()

    assertThat(results.size).isEqualTo(1)
    val found = results.results.first()
    assertThat(found.crn).isEqualTo(crn)
    assertThat(found.highlights).containsExactlyInAnyOrderEntriesOf(
      mapOf(
        "type" to listOf("Matches should be <em>highlighted</em>"),
        "outcome" to listOf("Matches were <em>highlighted</em>"),
      ),
    )
  }

  companion object {
    @JvmStatic
    fun crnsForFind() = listOf("T123456", "N123456")

    @JvmStatic
    fun datesForFind() = listOf("2023-01-01", "01-01-2023", "1/1/23", "01/01/2023", "1st Jan 2023")

    private val TEMPLATE_JSON = ResourceUtils.getFile("classpath:searchdata/contact-template.json").readText()
  }

  private fun RequestSpecification.authorised(): RequestSpecification =
    this.auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_PROBATION_CONTACT_SEARCH"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)

  private fun RequestSpecification.search(csr: ContactSearchRequest, queryParams: Map<String, Any> = mapOf()) =
    this.authorised().body(csr).queryParams(queryParams).post("/search/contacts")

  private fun ValidatableResponse.results() =
    this.statusCode(200)
      .extract().body()
      .`as`(ContactSearchResponse::class.java)
}
