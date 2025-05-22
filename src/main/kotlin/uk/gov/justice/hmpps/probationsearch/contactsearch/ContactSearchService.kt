package uk.gov.justice.hmpps.probationsearch.contactsearch

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.Context
import io.opentelemetry.extension.kotlin.asContextElement
import io.opentelemetry.instrumentation.annotations.WithSpan
import io.sentry.Sentry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.withTimeoutOrNull
import org.opensearch.client.json.JsonData
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.Refresh
import org.opensearch.client.opensearch._types.SortOptions
import org.opensearch.client.opensearch._types.VersionType
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery
import org.opensearch.client.opensearch._types.query_dsl.ChildScoreMode
import org.opensearch.client.opensearch._types.query_dsl.HybridQuery
import org.opensearch.client.opensearch._types.query_dsl.NestedQuery
import org.opensearch.client.opensearch._types.query_dsl.Query.Builder
import org.opensearch.client.opensearch.core.BulkRequest
import org.opensearch.client.opensearch.core.SearchRequest
import org.opensearch.client.opensearch.core.bulk.BulkOperation
import org.opensearch.client.opensearch.core.search.HighlighterEncoder
import org.opensearch.client.opensearch.core.search.TrackHits
import org.opensearch.data.client.orhlc.NativeSearchQuery
import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder
import org.opensearch.data.client.orhlc.OpenSearchRestTemplate
import org.opensearch.index.query.BoolQueryBuilder
import org.opensearch.index.query.Operator
import org.opensearch.index.query.QueryBuilders.boolQuery
import org.opensearch.index.query.QueryBuilders.matchQuery
import org.opensearch.index.query.QueryBuilders.simpleQueryStringQuery
import org.opensearch.index.query.SimpleQueryStringFlag
import org.opensearch.search.fetch.subphase.highlight.HighlightBuilder
import org.opensearch.search.sort.FieldSortBuilder
import org.opensearch.search.sort.SortBuilders
import org.opensearch.search.sort.SortOrder.ASC
import org.opensearch.search.sort.SortOrder.DESC
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.probationsearch.IndexNotReadyException
import uk.gov.justice.hmpps.probationsearch.contactsearch.ContactSearchService.SortType
import uk.gov.justice.hmpps.probationsearch.contactsearch.ContactSearchService.SortType.Companion.AI_SEARCH_HIGHLIGHT
import uk.gov.justice.hmpps.probationsearch.contactsearch.ContactSearchService.SortType.LAST_UPDATED_DATETIME
import uk.gov.justice.hmpps.probationsearch.contactsearch.ContactSearchService.SortType.SCORE
import uk.gov.justice.hmpps.probationsearch.services.DeliusService
import uk.gov.justice.hmpps.sqs.audit.HmppsAuditService
import java.io.StringReader
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import org.opensearch.client.opensearch._types.SortOrder as JavaClientSortOrder
import org.opensearch.client.opensearch._types.query_dsl.Operator as JavaClientOperator

@Service
class ContactSearchService(
  private val restTemplate: OpenSearchRestTemplate,
  private val auditService: HmppsAuditService?,
  private val objectMapper: ObjectMapper,
  private val deliusService: DeliusService,
  private val openSearchClient: OpenSearchClient,
  private val telemetryClient: TelemetryClient,
) {
  companion object {
    const val CONTACT_SEMANTIC_SEARCH_PRIMARY = "contact-semantic-search-primary"
  }

  private val scope = CoroutineScope(Context.current().asContextElement())

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

  fun checkIndexIsNotFound(indexName: String, maxRetries: Int = 1, timeout: Duration = Duration.ofSeconds(5))  {
    (0..maxRetries).forEach { count ->
      if (!openSearchClient.indices().exists { it.index(indexName) }.value()) {
        return
      } else {
        if (count == maxRetries) {
          throw IndexNotReadyException("Index is still blocked by $indexName")
        }
        TimeUnit.MILLISECONDS.sleep(timeout.toMillis())
      }
    }
  }

  suspend fun semanticSearch(request: ContactSearchRequest, pageable: Pageable): ContactSearchResponse {
    checkIndexIsNotFound("block-${request.crn.lowercase()}")
    audit(request, pageable)
    val crnExists = openSearchClient.search(
      { searchRequest ->
        searchRequest.index(CONTACT_SEMANTIC_SEARCH_PRIMARY)
          .routing(request.crn)
          .query { q -> q.matchesCrn(request.crn) }
          .trackTotalHits(TrackHits.of { it.count(1) })
          .size(0)
      },
      Any::class.java,
    ).hits().total().value() > 0
    if (!crnExists) {
      val loadDataJob = scope.launch { loadDataRetry(request.crn) }
      withTimeoutOrNull(Duration.ofSeconds(30)) { loadDataJob.join() }
        ?: throw IndexNotReadyException("Timed out waiting for contacts with CRN=${request.crn} to be indexed for semantic search. The indexing process has not been interrupted.")
    }

    val query = if (request.query.isNotEmpty()) {
      val keywordQuery = BoolQuery.of { bool ->
        bool
          .filter { it.matchesCrn(request.crn) }
          .must { must ->
            must.simpleQueryString { simpleQueryString ->
              simpleQueryString.query(request.query)
                .analyzeWildcard(true)
                .defaultOperator(if (request.matchAllTerms) JavaClientOperator.And else JavaClientOperator.Or)
                .fields("notes", "type", "outcome", "description")
                .flags { it.multiple("AND|OR|PREFIX|PHRASE|PRECEDENCE|ESCAPE|FUZZY|SLOP") }
            }
          }
      }.toQuery()
      val semanticQuery = NestedQuery.of { nested ->
        nested
          .scoreMode(ChildScoreMode.Max)
          .path("textEmbedding")
          .query { query ->
            query.neural {
              it.field("textEmbedding.knn")
                .queryText(request.query)
                .minScore(0.793F)
                .filter(Builder().matchesCrn(request.crn).build())
            }
          }
      }.toQuery()
      HybridQuery.of { hybrid -> hybrid.queries(keywordQuery, semanticQuery) }
    } else {
      BoolQuery.of { bool -> bool.filter { it.matchesCrn(request.crn) } }
    }.toQuery()


    val searchRequest = SearchRequest.of { searchRequest ->
      searchRequest
        .index(CONTACT_SEMANTIC_SEARCH_PRIMARY)
        .routing(request.crn)
        .timeout("15s")
        .source { source ->
          source.filter {
            it.includes(
              "crn",
              "id",
              "typeCode",
              "typeDescription",
              "outcomeCode",
              "outcomeDescription",
              "description",
              "notes",
              "date",
              "startTime",
              "endTime",
              "lastUpdatedDateTime",
            )
          }
        }
        .query(query)
        .trackTotalHits(TrackHits.of { it.count(5000) })
        .size(pageable.pageSize)
        .from(pageable.offset.toInt())
        .sort(buildSortOptions(pageable.sort.fieldSorts()))
        .highlight { highlight ->
          highlight
            .encoder(HighlighterEncoder.Html)
            .fields("notes") { it }
            .fields("type") { it }
            .fields("outcome") { it }
            .fields("description") { it }
            .fragmentSize(200)
        }
    }


    val searchResponse = openSearchClient.search(searchRequest, ContactSearchResult::class.java)
    val results = searchResponse.hits().hits().mapNotNull {
      it.source()?.copy(
        highlights = it.highlight().ifEmpty { mapOf("notes" to listOf(AI_SEARCH_HIGHLIGHT)) },
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

  private fun loadDataRetry(crn: String, maxRetries: Int = 2 ) {
    (0..maxRetries).forEach { count ->
      try {
        loadData(crn)
        return
      } catch (ex: Exception) {
        if (count == maxRetries) {
          openSearchClient.deleteByQuery {
            it.index(CONTACT_SEMANTIC_SEARCH_PRIMARY)
              .query { q -> q.matchesCrn(crn) }
              .routing(crn)
          }
          val exception: Exception = RuntimeException("Failed to load data")
          Sentry.captureException(exception)
          telemetryClient.trackException(exception)
          throw exception
        }
      }
    }
  }

  @WithSpan
  private fun loadData(crn: String) {
    val startTime = System.currentTimeMillis()

    openSearchClient.indices().create {
      it.index("block-${crn.lowercase()}")
        .settings{settings ->
          settings.index{index ->
            index
              .numberOfShards("1")
              .numberOfReplicas("0")}}
    }

    val mapper = openSearchClient._transport().jsonpMapper()
    val operations = deliusService.getContacts(crn).map { contact ->
      BulkOperation.of { bulk ->
        bulk.index {
          it.id(contact.contactId.toString())
            .version(contact.version)
            .versionType(VersionType.External)
            .document(JsonData.from(mapper.jsonProvider().createParser(StringReader(contact.json)), mapper))
        }
      }
    }
    val request = BulkRequest.Builder()
      .index(CONTACT_SEMANTIC_SEARCH_PRIMARY)
      .routing(crn)
      .refresh(Refresh.True)
      .timeout { it.time("5m") }
      .operations(operations)
      .build()
    openSearchClient.bulk(request)
    openSearchClient.indices().delete{it.index("block-${crn.lowercase()}")}
    telemetryClient.trackEvent(
      "OnDemandDataLoad",
      mapOf("crn" to crn),
      mapOf(
        "duration" to (System.currentTimeMillis() - startTime).toDouble(),
        "count" to request.operations().size.toDouble(),
      ),
    )
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

  private fun buildSortOptions(fieldSorts: List<FieldSortBuilder>): List<SortOptions> {
    return when (fieldSorts.size) {
      0 -> {
        listOf(
          SortOptions.Builder().field { f -> f.field(SCORE.searchField).order(JavaClientSortOrder.Desc) }.build(),
          SortOptions.Builder()
            .field { f -> f.field(LAST_UPDATED_DATETIME.searchField).order(JavaClientSortOrder.Desc) }.build(),
        )
      }

      else -> fieldSorts.map {
        SortOptions.Builder().field { f ->
          f.field(it.fieldName).order(if (it.order() == DESC) JavaClientSortOrder.Desc else JavaClientSortOrder.Asc)
        }.build()
      }
    }
  }

  enum class SortType(val aliases: List<String>, val searchField: String) {
    DATE(listOf("date", "CONTACT_DATE"), "date.date"),
    LAST_UPDATED_DATETIME(listOf("lastUpdated"), "lastUpdatedDateTime"),
    SCORE(listOf("relevance", "RELEVANCE"), "_score"),
    ;

    companion object {
      const val AI_SEARCH_HIGHLIGHT =
        "<div class='badge bg-secondary' title='This result was found using AI-powered search. Use the view link to see the full notes.' style='font-size: 12px'>✨ AI Text Match</div>"

      fun from(searchField: String): SortType? = entries.firstOrNull { it.searchField == searchField }
    }
  }
}

private fun Builder.matchesCrn(crn: String) =
  this.term { term -> term.field("crn").value { it.stringValue(crn) } }

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
  Sort.Direction.ASC -> ASC
  Sort.Direction.DESC -> DESC
}

private fun Sort.fieldSorts() = SortType.entries.flatMap { type ->
  type.aliases.mapNotNull { alias ->
    getOrderFor(alias)?.let {
      SortBuilders.fieldSort(type.searchField).order(it.direction.toSortOrder())
    }
  }
}.ifEmpty { listOf(SortBuilders.fieldSort(SCORE.searchField).order(DESC)) }

private fun NativeSearchQueryBuilder.sorted(sorts: List<FieldSortBuilder>): NativeSearchQuery {
  sorted(sorts) { this.withSorts(it) }
  return this.build()
}

private fun sorted(sorts: List<FieldSortBuilder>, sortFn: (List<FieldSortBuilder>) -> Unit) {
  when (sorts.size) {
    0 -> {
      sortFn(
        listOf(
          SortBuilders.fieldSort(SCORE.searchField).order(DESC),
          SortBuilders.fieldSort(LAST_UPDATED_DATETIME.searchField).order(DESC),
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
