package uk.gov.justice.hmpps.probationsearch.contactsearch.activitysearch

import org.opensearch.data.client.orhlc.NativeSearchQuery
import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder
import org.opensearch.data.client.orhlc.OpenSearchRestTemplate
import org.opensearch.index.query.BoolQueryBuilder
import org.opensearch.index.query.Operator
import org.opensearch.index.query.QueryBuilder
import org.opensearch.index.query.QueryBuilders.boolQuery
import org.opensearch.index.query.QueryBuilders.matchQuery
import org.opensearch.index.query.QueryBuilders.rangeQuery
import org.opensearch.index.query.QueryBuilders.simpleQueryStringQuery
import org.opensearch.index.query.SimpleQueryStringFlag
import org.opensearch.search.fetch.subphase.highlight.HighlightBuilder
import org.opensearch.search.sort.FieldSortBuilder
import org.opensearch.search.sort.SortBuilders
import org.opensearch.search.sort.SortOrder
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.probationsearch.contactsearch.audit.ContactSearchAuditService
import uk.gov.justice.hmpps.probationsearch.contactsearch.extensions.OpenSearchRestClientExtensions.fieldSorts
import uk.gov.justice.hmpps.probationsearch.services.mustAll
import uk.gov.justice.hmpps.probationsearch.services.shouldAll
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class ActivitySearchService(
  private val restTemplate: OpenSearchRestTemplate,
  private val auditService: ContactSearchAuditService,
) {
  fun activitySearch(request: ActivitySearchRequest, pageable: Pageable): ActivitySearchResponse {
    auditService.audit(request, pageable)

    val indexName = "contact-search-primary"
    val activityQuery = activitySearchQueryForRestClient(pageable, request)
    val searchResponse =
      restTemplate.search(activityQuery, ActivitySearchResult::class.java, IndexCoordinates.of(indexName))
    val results = searchResponse.searchHits.mapNotNull {
      it.content.copy(
        highlights = it.highlightFields,
      )
    }

    val response = PageImpl(results, pageable, searchResponse.totalHits)

    return ActivitySearchResponse(
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

  private fun BoolQueryBuilder.fromActivityRequest(request: ActivitySearchRequest): BoolQueryBuilder {
    must(matchQuery("crn", request.crn))
    request.dateFrom?.let {
      filter(rangeQuery("date").gte(it.toString()))
    }
    request.dateTo?.let {
      if (it.isEqual(LocalDate.now())) {
        filter(rangeQuery("startDateTime").lte(LocalDateTime.now()))
      } else {
        filter(rangeQuery("date").lte(it.toString()))
      }
    }

    if (request.dateTo == null) {
      filter(rangeQuery("startDateTime").lte(LocalDateTime.now()))
    }

    val filters = ActivityFilter.entries.filter { request.filters.contains(it.filterName) }
      .flatMap { it.queries }
    if (filters.isNotEmpty()) {
      shouldAll(filters).minimumShouldMatch(1)
    }

    if (request.keywords?.isNotEmpty() == true) {
      must(
        simpleQueryStringQuery(request.keywords)
          .analyzeWildcard(true)
          .defaultOperator(Operator.OR)
          .field("notes")
          .field("type")
          .field("outcome")
          .field("description")
          .field("complied")
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

  enum class ActivityFilter(val filterName: String, val queries: List<QueryBuilder>) {
    COMPLIED("complied", listOf(matchQuery("complied", "complied"))),
    NOT_COMPLIED("notComplied", listOf(matchQuery("complied", "ftc"))),
    NO_OUTCOME(
      "noOutcome",
      listOf(boolQuery().mustAll(listOf(matchQuery("requiresOutcome", "Y"), matchQuery("outcomeRequiredFlag", "Y")))),
    )
  }

  enum class SortType(val aliases: List<String>, val searchField: String) {
    DATE(listOf("date", "CONTACT_DATE"), "date.date"),
    START_DATE_TIME(listOf("startDateTime"), "startDateTime"),
    START_TIME(listOf("startTime"), "startTime"),
    SCORE(listOf("relevance", "RELEVANCE"), "_score"),
    ;

    companion object {
      fun from(searchField: String): SortType? = entries.firstOrNull { it.searchField == searchField }
    }
  }

  private fun NativeSearchQueryBuilder.sorted(sorts: List<FieldSortBuilder>): NativeSearchQuery {
    when (sorts.size) {
      0 -> this.withSorts(listOf(SortBuilders.fieldSort(SortType.START_DATE_TIME.searchField).order(SortOrder.DESC)))
      else -> this.withSorts(sorts)
    }
    return this.build()
  }
}
