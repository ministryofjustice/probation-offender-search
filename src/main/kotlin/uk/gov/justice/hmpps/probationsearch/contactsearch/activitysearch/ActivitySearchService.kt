package uk.gov.justice.hmpps.probationsearch.contactsearch.activitysearch

import com.fasterxml.jackson.databind.ObjectMapper
import io.opentelemetry.api.trace.Span
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder
import org.opensearch.data.client.orhlc.OpenSearchRestTemplate
import org.opensearch.index.query.BoolQueryBuilder
import org.opensearch.index.query.QueryBuilder
import org.opensearch.index.query.QueryBuilders
import org.opensearch.index.query.QueryBuilders.boolQuery
import org.opensearch.index.query.QueryBuilders.matchQuery
import org.opensearch.index.query.QueryBuilders.rangeQuery
import org.opensearch.index.query.QueryBuilders.simpleQueryStringQuery
import org.opensearch.index.query.SimpleQueryStringFlag
import org.opensearch.search.fetch.subphase.highlight.HighlightBuilder
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.probationsearch.contactsearch.ContactSearchResponse
import uk.gov.justice.hmpps.probationsearch.contactsearch.ContactSearchResult
import uk.gov.justice.hmpps.probationsearch.contactsearch.ContactSearchService
import uk.gov.justice.hmpps.probationsearch.contactsearch.fieldSorts
import uk.gov.justice.hmpps.probationsearch.contactsearch.sorted
import uk.gov.justice.hmpps.probationsearch.services.DeliusService
import uk.gov.justice.hmpps.sqs.audit.HmppsAuditService
import java.time.Instant
import java.time.LocalDate

@Service
class ActivitySearchService(
  private val restTemplate: OpenSearchRestTemplate,
  private val auditService: HmppsAuditService?,
  private val objectMapper: ObjectMapper,
  private val deliusService: DeliusService,
) {

  private val scope = CoroutineScope(Dispatchers.IO)

  fun activitySearch(request: ActivitySearchRequest, pageable: Pageable): ContactSearchResponse {
    audit(request, pageable)

    val indexName = "contact-search-primary"
    val activityQuery = activitySearchQueryForRestClient(pageable, request)
    val searchResponse =
      restTemplate.search(activityQuery, ContactSearchResult::class.java, IndexCoordinates.of(indexName))
    val results = searchResponse.searchHits.mapNotNull {
      it.content.copy(
        highlights = it.highlightFields,
      )
    }

    val response = PageImpl(results, pageable, searchResponse.totalHits)

    return ContactSearchResponse(
      response.numberOfElements,
      response.pageable.pageNumber,
      response.totalElements,
      response.totalPages,
      results,
    )
  }

  private fun activitySearchQueryForRestClient(
    pageable: Pageable,
    request: ActivitySearchRequest,
  ) = NativeSearchQueryBuilder()
    .withQuery(boolQuery().fromActivityRequest(request))
    .withPageable(PageRequest.of(pageable.pageNumber, pageable.pageSize))
    .withTrackTotalHits(true)
    .withHighlightBuilder(
      HighlightBuilder()
        .encoder("html")
        .field("notes")
        .field("type")
        .field("outcome")
        .field("description")
        .fragmentSize(200),
    ).sorted(pageable.sort.fieldSorts())

  private fun audit(request: ActivitySearchRequest, pageable: Pageable) {
    val name = SecurityContextHolder.getContext().authentication.name
    auditService?.run {
      scope.launch {
        publishEvent(
          what = "Search Contacts for Activity Log",
          who = name,
          `when` = Instant.now(),
          subjectId = request.crn,
          subjectType = "CRN",
          correlationId = Span.current().spanContext.traceId,
          service = "probation-search",
          details = objectMapper.writeValueAsString(request),
        )
      }
    }

    val fieldSorts = pageable.sort.fieldSorts()
    scope.launch {
      deliusService.auditActivitySearch(
        ActivitySearchAuditRequest(
          request,
          name,
          ActivitySearchAuditRequest.PageRequest(
            pageable.pageNumber,
            pageable.pageSize,
            fieldSorts.mapNotNull { ContactSearchService.SortType.from(it.fieldName)?.aliases?.first() }.joinToString(),
            fieldSorts.joinToString { it.order().toString() },
          ),
        ),
      )
    }
  }

  enum class ActivityFilter(val filterName: String, val queries: List<QueryBuilder>) {
    COMPLIED("complied", listOf(matchQuery("complied", "complied"))),
    NOT_COMPLIED("notComplied", listOf(matchQuery("complied", "ftc"))),
    NO_OUTCOME(
      "noOutcome",
      listOf(
        boolQuery().mustNot(QueryBuilders.existsQuery("outcome")),
        rangeQuery("date").lte(LocalDate.now().toString()),
      ),
    )
  }
}

private fun BoolQueryBuilder.fromActivityRequest(request: ActivitySearchRequest): BoolQueryBuilder {
  filter(matchQuery("crn", request.crn))
  request.dateFrom?.let { filter(rangeQuery("date").gte(it.toString())) }
  request.dateTo?.let { filter(rangeQuery("date").lte(it.toString())) }
  ActivitySearchService.ActivityFilter.entries.filter { request.filters.contains(it.filterName) }.forEach {
    it.queries.forEach { q -> filter(q) }
  }
  if (request.keywords?.isNotEmpty() == true) {
    must(
      simpleQueryStringQuery(request.keywords)
        .analyzeWildcard(true)
        .defaultOperator(org.opensearch.index.query.Operator.OR)
        .field("notes")
        .field("type")
        .field("outcome")
        .field("description")
        .flags(
          SimpleQueryStringFlag.AND,
          SimpleQueryStringFlag.OR,
          SimpleQueryStringFlag.PREFIX,
          SimpleQueryStringFlag.PHRASE,
          SimpleQueryStringFlag.PRECEDENCE,
          SimpleQueryStringFlag.ESCAPE,
          SimpleQueryStringFlag.FUZZY,
          SimpleQueryStringFlag.SLOP,
        ),
    )
  }
  return this
}