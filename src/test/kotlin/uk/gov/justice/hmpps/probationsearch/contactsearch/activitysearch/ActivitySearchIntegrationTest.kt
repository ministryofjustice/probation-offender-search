package uk.gov.justice.hmpps.probationsearch.contactsearch.activitysearch

import io.restassured.RestAssured
import io.restassured.response.ValidatableResponse
import io.restassured.specification.RequestSpecification
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.opensearch.action.admin.indices.alias.Alias
import org.opensearch.action.admin.indices.delete.DeleteIndexRequest
import org.opensearch.client.RequestOptions
import org.opensearch.client.indices.CreateIndexRequest
import org.opensearch.client.indices.GetIndexRequest
import org.opensearch.client.indices.PutIndexTemplateRequest
import org.opensearch.common.xcontent.XContentType
import org.opensearch.core.xcontent.MediaType
import org.opensearch.data.client.orhlc.OpenSearchRestTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.data.elasticsearch.core.query.Query
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.util.ResourceUtils
import uk.gov.justice.hmpps.probationsearch.contactsearch.activitysearch.ActivityGenerator.contacts
import uk.gov.justice.hmpps.probationsearch.services.FeatureFlags
import uk.gov.justice.hmpps.probationsearch.util.JwtAuthenticationHelper
import uk.gov.justice.hmpps.probationsearch.wiremock.DeliusApiExtension
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(DeliusApiExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@MockitoBean(types = [FeatureFlags::class])
@ActiveProfiles(profiles = ["test"])
class ActivitySearchIntegrationTest {

  @Autowired
  internal lateinit var jwtAuthenticationHelper: JwtAuthenticationHelper

  @Autowired
  internal lateinit var openSearchRestTemplate: OpenSearchRestTemplate

  @Value($$"${local.server.port}")
  internal val port: Int = 0

  internal val deliusApiMock = DeliusApiExtension.deliusApi

  @BeforeEach
  internal fun before() {
    RestAssured.port = port
    deliusApiMock.stubDeliusAuditSuccess()
    val indexName = "contact-search-a"
    val aliasName = "contact-search-primary"
    openSearchRestTemplate.execute {
      it.indices().putTemplate(
        PutIndexTemplateRequest("contact-search-template").source(
          TEMPLATE_JSON,
          MediaType.fromMediaType(XContentType.JSON.mediaType()),
        ),
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
  fun `can retrieve paginated results with only crn`() {
    val crn = "T654321"
    val results = RestAssured.given()
      .`when`()
      .search(ActivitySearchRequest(crn), mapOf("page" to 0, "size" to 3))
      .then()
      .results()

    assertThat(results.size).isEqualTo(3)
    assertThat(results.totalResults).isEqualTo(6)
    assertThat(results.results.map { it.id }).containsAll(
      contacts
        .filter { it.crn == crn && it.startDateTime!! <= LocalDateTime.now() }
        .sortedByDescending { it.startDateTime }
        .map { it.id }
        .take(3),
    )
  }


  @Test
  fun `can retrieve all results with only crn`() {
    val crn = "T654321"
    val results = RestAssured.given()
      .`when`()
      .search(ActivitySearchRequest(crn), mapOf("page" to 0, "size" to 20))
      .then()
      .results()

    assertThat(results.size).isEqualTo(6)
    assertThat(results.totalResults).isEqualTo(6)
  }


  @Test
  fun `can retrieve only those that have no outcome with only crn`() {
    val crn = "T654321"
    val results = RestAssured.given()
      .`when`()
      .search(
        ActivitySearchRequest(crn, filters = listOf(ActivitySearchService.ActivityFilter.NO_OUTCOME.filterName)),
        mapOf("page" to 0, "size" to 20),
      )
      .then()
      .results()

    assertThat(results.size).isEqualTo(1)
    assertThat(results.totalResults).isEqualTo(1)
  }

  @Test
  fun `can retrieve only those that have no outcome or complied with only crn`() {
    val crn = "T654321"
    val results = RestAssured.given()
      .`when`()
      .search(
        ActivitySearchRequest(
          crn,
          filters = listOf(
            ActivitySearchService.ActivityFilter.NO_OUTCOME.filterName,
            ActivitySearchService.ActivityFilter.COMPLIED.filterName,
          ),
        ),
        mapOf("page" to 0, "size" to 20),
      )
      .then()
      .results()

    assertThat(results.size).isEqualTo(2)
    assertThat(results.totalResults).isEqualTo(2)
  }

  @Test
  fun `can retrieve only those that have no outcome, complied or ftc with only crn`() {
    val crn = "T654321"
    val results = RestAssured.given()
      .`when`()
      .search(
        ActivitySearchRequest(
          crn,
          filters = listOf(
            ActivitySearchService.ActivityFilter.NO_OUTCOME.filterName,
            ActivitySearchService.ActivityFilter.COMPLIED.filterName,
            ActivitySearchService.ActivityFilter.NOT_COMPLIED.filterName,
          ),
        ),
        mapOf("page" to 0, "size" to 20),
      )
      .then()
      .results()

    assertThat(results.size).isEqualTo(3)
    assertThat(results.totalResults).isEqualTo(3)
  }


  @Test
  fun `can retrieve second page of paginated results with only crn`() {
    val crn = "T654321"
    val results = RestAssured.given()
      .`when`()
      .search(ActivitySearchRequest(crn), mapOf("page" to 1, "size" to 3))
      .then()
      .results()

    assertThat(results.size).isEqualTo(3)
    assertThat(results.totalResults).isEqualTo(6)
    assertThat(results.results.map { it.id }).isEqualTo(
      contacts
        .asSequence()
        .filter { it.crn == crn }
        .sortedWith(compareByDescending<ActivitySearchResult> { it.date }.thenByDescending { it.startTime })
        .map { it.id }
        .toList()
        .takeLast(3)
        .sorted(),
    )
  }

  @Test
  fun `can filter based on no outcome (and in the past)`() {
    val crn = "T654321"
    val results = RestAssured.given()
      .`when`()
      .search(
        ActivitySearchRequest(crn, filters = listOf(ActivitySearchService.ActivityFilter.NO_OUTCOME.filterName)),
        mapOf("page" to 0, "size" to 3),
      )
      .then()
      .results()

    assertThat(results.size).isEqualTo(1)
    assertThat(results.totalResults).isEqualTo(1)
    assertThat(results.results[0].notes).isEqualTo("I have no outcome")
  }

  @Test
  fun `can filter based on complied`() {
    val crn = "X123456"
    val results = RestAssured.given()
      .`when`()
      .search(
        ActivitySearchRequest(crn, filters = listOf(ActivitySearchService.ActivityFilter.COMPLIED.filterName)),
        mapOf("page" to 0, "size" to 3),
      )
      .then()
      .results()

    assertThat(results.size).isEqualTo(1)
    assertThat(results.totalResults).isEqualTo(1)
    assertThat(results.results[0].notes).isEqualTo("I complied")
  }

  @Test
  fun `can filter based on not complied`() {
    val crn = "T654321"
    val results = RestAssured.given()
      .`when`()
      .search(
        ActivitySearchRequest(
          crn,
          filters = listOf(ActivitySearchService.ActivityFilter.NOT_COMPLIED.filterName),
        ),
        mapOf("page" to 0, "size" to 3),
      )
      .then()
      .results()

    assertThat(results.size).isEqualTo(1)
    assertThat(results.totalResults).isEqualTo(1)
    assertThat(results.results[0].notes).isEqualTo("I failed to comply")
  }

  @Test
  fun `can filter based on not complied and compiled`() {
    val crn = "X123456"
    val results = RestAssured.given()
      .`when`()
      .search(
        ActivitySearchRequest(
          crn,
          filters = listOf(
            ActivitySearchService.ActivityFilter.NOT_COMPLIED.filterName,
            ActivitySearchService.ActivityFilter.COMPLIED.filterName,
          ),
        ),
        mapOf("page" to 0, "size" to 3),
      )
      .then()
      .results()

    assertThat(results.size).isEqualTo(2)
    assertThat(results.totalResults).isEqualTo(2)
    assertThat(results.results[0].notes).isEqualTo("I failed to comply")
    assertThat(results.results[1].notes).isEqualTo("I complied")
  }

  @Test
  fun `can filter based on not complied, compiled, no outcome with keywords`() {
    val crn = "T654321"
    val results = RestAssured.given()
      .`when`()
      .search(
        ActivitySearchRequest(
          crn,
          filters = listOf(
            ActivitySearchService.ActivityFilter.NOT_COMPLIED.filterName,
            ActivitySearchService.ActivityFilter.COMPLIED.filterName,
            ActivitySearchService.ActivityFilter.NO_OUTCOME.filterName,
          ),
          keywords = "special failed complied",
        ),
        mapOf("page" to 0, "size" to 3),
      )
      .then()
      .results()

    assertThat(results.size).isEqualTo(2)
    assertThat(results.totalResults).isEqualTo(2)
  }

  @Test
  fun `can filter based on not complied and not compiled with keywords`() {
    val crn = "X123456"
    val results = RestAssured.given()
      .`when`()
      .search(
        ActivitySearchRequest(
          crn,
          filters = listOf(
            ActivitySearchService.ActivityFilter.NOT_COMPLIED.filterName,
            ActivitySearchService.ActivityFilter.COMPLIED.filterName,
          ),
          keywords = "special failed complied",
        ),
        mapOf("page" to 0, "size" to 3),
      )
      .then()
      .results()

    assertThat(results.size).isEqualTo(2)
    assertThat(results.totalResults).isEqualTo(2)
    assertThat(results.results[0].notes).isEqualTo("I failed to comply")
    assertThat(results.results[1].notes).isEqualTo("I complied")
  }


  @Test
  fun `can filter based on date range where days are the same and only returns records for that day`() {
    val crn = "T654321"
    val results = RestAssured.given()
      .`when`()
      .search(
        ActivitySearchRequest(crn, dateFrom = LocalDate.now(), dateTo = LocalDate.now()),
        mapOf("page" to 0, "size" to 3),
      )
      .then()
      .results()

    assertThat(results.size).isEqualTo(3)
    assertThat(results.totalResults).isEqualTo(3)
    assertThat(results.results.map { it.notes }).contains("I have no outcome")
  }

  @Test
  fun `can filter based on date range with days from only`() {
    val crn = "T654321"
    val results = RestAssured.given()
      .`when`()
      .search(
        ActivitySearchRequest(crn, dateFrom = LocalDate.now()),
        mapOf("page" to 0, "size" to 6),
      )
      .then()
      .results()

    assertThat(results.size).isEqualTo(3)
    assertThat(results.totalResults).isEqualTo(3)
    assertThat(results.results.map { it.notes }).contains("I have no outcome")
  }

  @Test
  fun `can filter based on date range with days to only`() {
    val crn = "T654321"
    val results = RestAssured.given()
      .`when`()
      .search(
        ActivitySearchRequest(crn, dateTo = LocalDate.now()),
        mapOf("page" to 0, "size" to 4),
      )
      .then()
      .results()

    assertThat(results.size).isEqualTo(4)
    assertThat(results.totalResults).isEqualTo(6)
    assertThat(results.results.map { it.notes }).contains("I have no outcome")
  }

  @Test
  fun `can filter based on date range`() {
    val crn = "T654321"
    val results = RestAssured.given()
      .`when`()
      .search(
        ActivitySearchRequest(
          crn,
          dateFrom = LocalDate.now().minusDays(3),
          dateTo = LocalDate.now().minusDays(1),
        ),
        mapOf("page" to 0, "size" to 4),
      )
      .then()
      .results()

    assertThat(results.size).isEqualTo(3)
    assertThat(results.totalResults).isEqualTo(3)
    assertThat(results.results[0].typeCode).isEqualTo("TYPE_CODE1")
    assertThat(results.results[1].typeCode).isEqualTo("TYPE_CODE2")
    assertThat(results.results[2].typeCode).isEqualTo("TYPE_CODE3")
  }


  @Test
  fun `can filter based on key word with date range`() {
    val crn = "T654321"
    val results = RestAssured.given()
      .`when`()
      .search(
        ActivitySearchRequest(
          crn,
          keywords = "TYPE_CODE2 TYPE_CODE3",
          dateTo = LocalDate.now().minusDays(1),
        ),
        mapOf("page" to 0, "size" to 4),
      )
      .then()
      .results()

    assertThat(results.size).isEqualTo(2)
    assertThat(results.totalResults).isEqualTo(2)
    assertThat(results.results[0].typeCode).isEqualTo("TYPE_CODE2")
    assertThat(results.results[1].typeCode).isEqualTo("TYPE_CODE3")
    assertThat(results.results[1].highlights).isEqualTo(
      mapOf(
        "type" to listOf("<em>TYPE_CODE3</em>"),
      ),
    )
  }


  companion object {
    private val TEMPLATE_JSON =
      ResourceUtils.getFile("classpath:search-setup/contact-keyword-index-template.json").readText()
  }

  private fun RequestSpecification.authorised(): RequestSpecification =
    this.auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_PROBATION_CONTACT_SEARCH"))
      .contentType(APPLICATION_JSON_VALUE)

  private fun RequestSpecification.search(csr: ActivitySearchRequest, queryParams: Map<String, Any> = mapOf()) =
    this.authorised().body(csr).queryParams(queryParams).post("/search/activity")

  private fun ValidatableResponse.results() =
    this.statusCode(200)
      .extract().body()
      .`as`(ActivitySearchResponse::class.java)
}
