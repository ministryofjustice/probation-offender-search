package uk.gov.justice.hmpps.probationsearch.contactsearch

import com.fasterxml.jackson.databind.ObjectMapper
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.query_dsl.ChildScoreMode
import org.opensearch.client.opensearch._types.query_dsl.MatchQuery
import org.opensearch.client.opensearch._types.query_dsl.NestedQuery
import org.opensearch.client.opensearch._types.query_dsl.SimpleQueryStringQuery
import org.opensearch.client.opensearch.core.SearchRequest
import org.opensearch.client.opensearch.core.search.TrackHits
import org.opensearch.data.client.orhlc.NativeSearchQuery
import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder
import org.opensearch.data.client.orhlc.OpenSearchRestTemplate
import org.opensearch.index.query.BoolQueryBuilder
import org.opensearch.index.query.Operator
import org.opensearch.index.query.QueryBuilders.*
import org.opensearch.index.query.SimpleQueryStringFlag
import org.opensearch.search.fetch.subphase.highlight.HighlightBuilder
import org.opensearch.search.sort.FieldSortBuilder
import org.opensearch.search.sort.SortBuilders
import org.opensearch.search.sort.SortOrder
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.elasticsearch.core.IndexOperations
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.data.elasticsearch.core.query.IndexQuery
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.probationsearch.contactsearch.ContactSearchService.SortType
import uk.gov.justice.hmpps.probationsearch.contactsearch.ContactSearchService.SortType.LAST_UPDATED_DATETIME
import uk.gov.justice.hmpps.probationsearch.contactsearch.ContactSearchService.SortType.SCORE
import uk.gov.justice.hmpps.probationsearch.services.DeliusService
import uk.gov.justice.hmpps.sqs.audit.HmppsAuditService
import java.time.Instant
import org.opensearch.client.opensearch._types.SortOrder as JavaClientSortOrder
import org.opensearch.client.opensearch._types.query_dsl.Operator as JavaClientOperator

@Service
class ContactSearchService(
  private val restTemplate: OpenSearchRestTemplate,
  private val auditService: HmppsAuditService?,
  private val objectMapper: ObjectMapper,
  private val deliusService: DeliusService,
  private val openSearchClient: OpenSearchClient,
  @Value("\${bedrock.model.id}") private val bedrockModelId: String, // Temp, remove after upgrading to OpenSearch 2.16 - workaround for https://github.com/opensearch-project/OpenSearch/issues/15034
) {

  private val scope = CoroutineScope(Dispatchers.IO)

  fun keywordSearch(request: ContactSearchRequest, pageable: Pageable): ContactSearchResponse {
    audit(request, pageable)

    val indexName = "contact-search-primary"
    val keywordQuery = keywordQueryForRestClient(pageable, request)
    val searchResponse =
      restTemplate.search(keywordQuery, ContactSearchResult::class.java, IndexCoordinates.of(indexName))
    val results = searchResponse.searchHits.mapNotNull {
      it.content.copy(
        highlights = it.highlightFields,
        score = it.score.toDouble().takeIf { request.includeScores },
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

  fun semanticSearch(request: ContactSearchRequest, pageable: Pageable): ContactSearchResponse {
    audit(request, pageable)

    val indexName = "contact-semantic-search-${request.crn.lowercase()}"
    restTemplate.indexOps(IndexCoordinates.of(indexName)).apply {
      if (!exists()) {
        delete()
        create()
        loadData(request.crn)
        refresh()
      }
    }

    // Must use the newer Java client here, the old rest client doesn't support hybrid queries
    val keywordQuery = keywordQueryForJavaClient(request)
    val semanticQuery = NestedQuery.of { nested ->
      nested
        .scoreMode(ChildScoreMode.Max)
        .path("textEmbedding")
        .query { query ->
          query.neural {
            it.field("textEmbedding.knn")
              .queryText(request.query)
              .modelId(bedrockModelId)
              .k(10)
          }
        }
    }.toQuery()
    val searchRequest = SearchRequest.of { searchRequest ->
      searchRequest
        .index(indexName)
        .query { query -> query.hybrid { hybrid -> hybrid.queries(keywordQuery, semanticQuery) } }
        .trackTotalHits(TrackHits.of { it.count(5000) })
        .size(pageable.pageSize)
        .from(pageable.offset.toInt())
        .sorted(pageable.sort.fieldSorts())
    }
    val searchResponse = openSearchClient.search(searchRequest, ContactSearchResult::class.java)
    val results = searchResponse.hits().hits().mapNotNull {
      it.source()?.copy(
        highlights = it.highlight(),
        score = it.score().takeIf { request.includeScores },
      )
    }
    val response = PageImpl(results, pageable, searchResponse.hits().total().value())

    return ContactSearchResponse(
      response.numberOfElements,
      response.pageable.pageNumber,
      response.totalElements,
      response.totalPages,
      results,
    )
  }

  private fun keywordQueryForRestClient(
    pageable: Pageable,
    request: ContactSearchRequest,
  ) = NativeSearchQueryBuilder()
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
    ).sorted(pageable.sort.fieldSorts())

  private fun keywordQueryForJavaClient(request: ContactSearchRequest) = if (request.query.isNotEmpty()) {
    SimpleQueryStringQuery.of { simpleQueryString ->
      simpleQueryString.query(request.query)
        .analyzeWildcard(true)
        .defaultOperator(if (request.matchAllTerms) JavaClientOperator.And else JavaClientOperator.Or)
        .fields("notes", "type", "outcome", "description")
        .flags { it.multiple("AND|OR|PREFIX|PHRASE|PRECEDENCE|ESCAPE|FUZZY|SLOP") }
    }
  } else {
    MatchQuery.of { match -> match.field("crn").query { it.stringValue(request.crn) } }
  }.toQuery()

  @WithSpan
  private fun IndexOperations.loadData(crn: String) {
    val documents = deliusService.getContacts(crn).map {
      IndexQuery().apply {
        id = it.contactId.toString()
        source = it.json
      }
    }

    restTemplate.bulkIndex(documents, indexCoordinates)
  }

  private fun audit(request: ContactSearchRequest, pageable: Pageable) {
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
  filter(matchQuery("crn", request.crn))
  if (request.query.isNotEmpty()) {
    must(
      simpleQueryStringQuery(request.query)
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

private fun SearchRequest.Builder.sorted(sorts: List<FieldSortBuilder>): SearchRequest.Builder {
  sorted(sorts) { fieldSorts ->
    this.sort {
      fieldSorts.forEach { fieldSort ->
        it.field { field ->
          field
            .field(fieldSort.fieldName)
            .order(if (fieldSort.order() == SortOrder.DESC) JavaClientSortOrder.Desc else JavaClientSortOrder.Asc)
        }
      }
      it
    }
  }
  return this
}

private fun NativeSearchQueryBuilder.sorted(sorts: List<FieldSortBuilder>): NativeSearchQuery {
  sorted(sorts) { this.withSorts(it) }
  return this.build()
}

private fun sorted(sorts: List<FieldSortBuilder>, sortFn: (List<FieldSortBuilder>) -> Unit) {
  when (sorts.size) {
    0 -> {
      sortFn(
        listOf(
          SortBuilders.fieldSort(SCORE.searchField).order(SortOrder.DESC),
          SortBuilders.fieldSort(LAST_UPDATED_DATETIME.searchField).order(SortOrder.DESC),
        ),
      )
    }

    1 -> {
      val sorted = sorts.first()
      sortFn(
        listOf(
          sorted,
          when (sorted.fieldName) {
            in SCORE.aliases -> SortBuilders.fieldSort(LAST_UPDATED_DATETIME.searchField).order(sorted.order())
            else -> SortBuilders.fieldSort(SCORE.searchField).order(sorted.order())
          },
        ),
      )
    }

    else -> sortFn(sorts)
  }
}
