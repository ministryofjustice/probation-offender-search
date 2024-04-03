package uk.gov.justice.hmpps.probationsearch.contactsearch

import com.fasterxml.jackson.databind.ObjectMapper
import io.opentelemetry.api.trace.Span
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.opensearch.data.client.orhlc.NativeSearchQuery
import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder
import org.opensearch.data.client.orhlc.OpenSearchRestTemplate
import org.opensearch.index.query.BoolQueryBuilder
import org.opensearch.index.query.Operator
import org.opensearch.index.query.QueryBuilders
import org.opensearch.index.query.QueryBuilders.boolQuery
import org.opensearch.index.query.SimpleQueryStringFlag
import org.opensearch.search.fetch.subphase.highlight.HighlightBuilder
import org.opensearch.search.sort.FieldSortBuilder
import org.opensearch.search.sort.SortBuilders
import org.opensearch.search.sort.SortOrder
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.probationsearch.contactsearch.ContactSearchService.SortType
import uk.gov.justice.hmpps.probationsearch.contactsearch.ContactSearchService.SortType.LAST_UPDATED_DATETIME
import uk.gov.justice.hmpps.probationsearch.contactsearch.ContactSearchService.SortType.SCORE
import uk.gov.justice.hmpps.probationsearch.services.DeliusService
import uk.gov.justice.hmpps.sqs.audit.HmppsAuditService
import java.time.Instant

@Service
class ContactSearchService(
  private val restTemplate: OpenSearchRestTemplate,
  private val auditService: HmppsAuditService?,
  private val objectMapper: ObjectMapper,
  private val deliusService: DeliusService,
) {

  private val scope = CoroutineScope(Dispatchers.IO)
  fun performSearch(request: ContactSearchRequest, pageable: Pageable): ContactSearchResponse {
    val name = SecurityContextHolder.getContext().authentication.name
    auditService?.run {
      scope.launch {
        publishEvent(
          what = "Search Contacts",
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
      deliusService.auditContactSearch(
        ContactSearchAuditRequest(
          request,
          name,
          ContactSearchAuditRequest.PageRequest(
            pageable.pageNumber,
            pageable.pageSize,
            fieldSorts.mapNotNull { SortType.from(it.fieldName)?.aliases?.first() }.joinToString(),
            fieldSorts.joinToString { it.order().toString() },
          ),
        ),
      )
    }

    val query: NativeSearchQuery = NativeSearchQueryBuilder()
      .withQuery(boolQuery().fromRequest(request))
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
      ).sorted(fieldSorts)

    val searchResponse =
      restTemplate.search(query, ContactSearchResult::class.java, IndexCoordinates.of("contact-search-primary"))
    val results = searchResponse.searchHits.mapNotNull { it.content.copy(highlights = it.highlightFields) }

    val response = PageImpl(results, pageable, searchResponse.totalHits)

    return ContactSearchResponse(
      response.numberOfElements,
      response.pageable.pageNumber,
      response.totalElements,
      response.totalPages,
      results,
    )
  }

  enum class SortType(val aliases: List<String>, val searchField: String) {
    DATE(listOf("date", "CONTACT_DATE"), "date.date"),
    LAST_UPDATED_DATETIME(listOf("lastUpdated"), "lastUpdatedDateTime"),
    SCORE(listOf("relevance", "RELEVANCE"), "_score"),
    ;

    companion object {
      fun from(searchField: String): SortType? = entries.firstOrNull { it.searchField == searchField }
    }
  }
}

private fun BoolQueryBuilder.fromRequest(request: ContactSearchRequest): BoolQueryBuilder {
  filter(QueryBuilders.matchQuery("crn", request.crn))
  if (request.query.isNotEmpty()) {
    must(
      QueryBuilders
        .simpleQueryStringQuery(request.query)
        .analyzeWildcard(true)
        .defaultOperator(if (request.matchAllTerms) Operator.AND else Operator.OR)
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

private fun Sort.Direction.toSortOrder() = when (this) {
  Sort.Direction.ASC -> SortOrder.ASC
  Sort.Direction.DESC -> SortOrder.DESC
}

private fun Sort.fieldSorts() = SortType.entries.flatMap { type ->
  type.aliases.mapNotNull { alias ->
    getOrderFor(alias)?.let {
      SortBuilders.fieldSort(type.searchField).order(it.direction.toSortOrder())
    }
  }
}

private fun NativeSearchQueryBuilder.sorted(sorts: List<FieldSortBuilder>): NativeSearchQuery {
  when (sorts.size) {
    0 -> {
      withSorts(
        SortBuilders.scoreSort().order(SortOrder.DESC),
        SortBuilders.fieldSort(LAST_UPDATED_DATETIME.searchField).order(SortOrder.DESC),
      )
    }

    1 -> {
      val sorted = sorts.first()
      withSorts(
        sorted,
        when (sorted.fieldName) {
          in SCORE.aliases -> SortBuilders.fieldSort(LAST_UPDATED_DATETIME.searchField).order(sorted.order())
          else -> SortBuilders.fieldSort(sorted.fieldName).order(sorted.order())
        },
      )
    }

    else -> withSorts(sorts)
  }
  return build()
}
