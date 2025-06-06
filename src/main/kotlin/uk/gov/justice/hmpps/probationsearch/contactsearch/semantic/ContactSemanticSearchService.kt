package uk.gov.justice.hmpps.probationsearch.contactsearch.semantic

import com.microsoft.applicationinsights.TelemetryClient
import io.opentelemetry.context.Context
import io.opentelemetry.extension.kotlin.asContextElement
import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.withTimeoutOrNull
import org.opensearch.client.json.JsonData
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.Refresh
import org.opensearch.client.opensearch._types.VersionType
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery
import org.opensearch.client.opensearch._types.query_dsl.ChildScoreMode
import org.opensearch.client.opensearch._types.query_dsl.HybridQuery
import org.opensearch.client.opensearch._types.query_dsl.NestedQuery
import org.opensearch.client.opensearch._types.query_dsl.Operator
import org.opensearch.client.opensearch._types.query_dsl.Query.Builder
import org.opensearch.client.opensearch.core.BulkRequest
import org.opensearch.client.opensearch.core.MsearchRequest
import org.opensearch.client.opensearch.core.SearchRequest
import org.opensearch.client.opensearch.core.bulk.BulkOperation
import org.opensearch.client.opensearch.core.msearch.RequestItem
import org.opensearch.client.opensearch.core.search.HighlightField
import org.opensearch.client.opensearch.core.search.HighlighterEncoder
import org.opensearch.client.opensearch.core.search.TrackHits
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.probationsearch.IndexNotReadyException
import uk.gov.justice.hmpps.probationsearch.contactsearch.extensions.OpenSearchJavaClientExtensions.buildSortOptions
import uk.gov.justice.hmpps.probationsearch.contactsearch.extensions.OpenSearchJavaClientExtensions.hits
import uk.gov.justice.hmpps.probationsearch.contactsearch.extensions.OpenSearchJavaClientExtensions.matchesCrn
import uk.gov.justice.hmpps.probationsearch.contactsearch.extensions.OpenSearchJavaClientExtensions.withPageable
import uk.gov.justice.hmpps.probationsearch.contactsearch.extensions.TextExtensions.asHighlightedFragmentOf
import uk.gov.justice.hmpps.probationsearch.contactsearch.extensions.TextExtensions.asTextChunks
import uk.gov.justice.hmpps.probationsearch.contactsearch.model.ContactSearchRequest
import uk.gov.justice.hmpps.probationsearch.contactsearch.model.ContactSearchResponse
import uk.gov.justice.hmpps.probationsearch.contactsearch.model.ContactSearchResult
import uk.gov.justice.hmpps.probationsearch.contactsearch.semantic.block.ContactBlockService
import uk.gov.justice.hmpps.probationsearch.services.DeliusService
import java.io.StringReader
import java.time.Duration

@Service
class ContactSemanticSearchService(
  private val deliusService: DeliusService,
  private val openSearchClient: OpenSearchClient,
  private val telemetryClient: TelemetryClient,
  private val blockService: ContactBlockService,
  @Value("\${dataload.ondemand.batch.size}")
  private val onDemandDataloadBatchSize: Int,
) {
  companion object {
    const val INDEX_NAME = "contact-semantic-search-primary"
    const val MIN_SEMANTIC_SCORE = 0.798F

    val KEYWORD_SEARCH_FIELDS = listOf("notes", "type", "outcome", "description")
    val RETURN_FIELDS = listOf(
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

  suspend fun search(request: ContactSearchRequest, pageable: Pageable): ContactSearchResponse {
    // Do not proceed if the CRN is "blocked". If the block is stale, rollback any partially loaded data and proceed
    blockService.checkIfBlockedOrRollbackIfStale(request.crn) { rollback { rollbackPartialLoad(request.crn) } }

    // If the CRN has not been indexed before, load all contacts on-demand before first search
    if (!crnExistsInIndex(request)) {
      val loadDataJobScope = CoroutineScope(Context.current().asContextElement())
      val loadDataJob = loadDataJobScope.launch {
        blockService.doWithBlock(request.crn, 2) {
          action { loadData(request.crn) }
          rollback { rollbackPartialLoad(request.crn) }
        }
      }
      withTimeoutOrNull(Duration.ofSeconds(30)) { loadDataJob.join() }
        ?: throw IndexNotReadyException("Timed out waiting for contacts with CRN=${request.crn} to be indexed for semantic search. The indexing process has not been interrupted.")
    }

    // Simplified query to return all contacts for the CRN when no query is passed
    if (request.query.isEmpty()) return emptyQuery(request, pageable)

    // Construct full hybrid semantic query
    val keywordQuery = BoolQuery.of { bool ->
      bool
        .filter { it.matchesCrn(request.crn) }
        .must { must ->
          must.simpleQueryString { simpleQueryString ->
            simpleQueryString.query(request.query)
              .analyzeWildcard(true)
              .defaultOperator(if (request.matchAllTerms) Operator.And else Operator.Or)
              .fields(KEYWORD_SEARCH_FIELDS)
              .flags { it.multiple("AND|OR|PREFIX|PHRASE|PRECEDENCE|ESCAPE|FUZZY|SLOP") }
          }
        }
    }.toQuery()
    val semanticQuery = NestedQuery.of { nested ->
      nested
        .scoreMode(ChildScoreMode.Max)
        .path("textEmbedding")
        .innerHits { innerHits -> innerHits.source { it.fetch(false) }.size(1) }
        .query { query ->
          query.neural {
            it.field("textEmbedding.knn")
              .queryText(request.query)
              .minScore(MIN_SEMANTIC_SCORE)
              .filter(Builder().matchesCrn(request.crn).build())
          }
        }
    }.toQuery()
    val hybridQuery =
      HybridQuery.of { hybrid -> hybrid.queries(keywordQuery, semanticQuery).paginationDepth(10000) }.toQuery()

    // Using "msearch" to execute two requests in parallel - hybrid + semantic-only (for highlighting)
    val response = openSearchClient.msearch(
      MsearchRequest.of { msearch ->
        msearch.searches(
          // Hybrid query:
          RequestItem.of { item ->
            item.header { it.index(INDEX_NAME).routing(request.crn) }
              .body { body ->
                body
                  .query(hybridQuery)
                  .source { source -> source.filter { it.includes(RETURN_FIELDS) } }
                  .withPageable(pageable)
                  .highlight { highlight ->
                    highlight
                      .encoder(HighlighterEncoder.Html)
                      .fields(KEYWORD_SEARCH_FIELDS.associateWith { HighlightField.of { it } })
                      .fragmentSize(200)
                  }
              }
          },
          // Additional semantic query, to get the relevant chunks (inner hits) for highlighting:
          // TODO remove this after upgrading to OpenSearch 3.0, which adds support for inner_hits on hybrid queries + semantic highlighting
          RequestItem.of { item ->
            item.header { it.index(INDEX_NAME).routing(request.crn) }
              .body { body ->
                body.query(semanticQuery)
                  .source { source -> source.fetch(false) }
                  .size(pageable.pageSize)
                  .sort(buildSortOptions(pageable.sort))
              }
          },
        )
      },
      ContactSearchResult::class.java,
    )

    val semanticInnerHits = response.responses()[1].hits()
    val results = response.responses()[0].hits().mapNotNull { hit ->
      hit.source()?.copy(
        semanticMatch = hit.highlight().isEmpty(),
        highlights = hit.highlight().ifEmpty {
          // If no highlight from OpenSearch, check if we have inner hits for the document by id
          hit.source()?.notes?.let { notes ->
            semanticInnerHits.firstOrNull { it.id() != null && it.id() == hit.id() }
              ?.let { hit -> hit.innerHits()["textEmbedding"]?.hits()?.hits()?.mapNotNull { it.nested()?.offset() } }
              ?.let { semanticChunkOffsets ->
                // Use the inner hit offsets to construct highlighted text fragments
                val chunks = notes.asTextChunks()
                val highlightedFragments = semanticChunkOffsets
                  .mapNotNull { offset -> chunks.elementAtOrNull(offset)?.asHighlightedFragmentOf(notes) }
                  .ifEmpty { return@let null }
                mapOf("notes" to highlightedFragments)
              }
          } ?: mapOf()
        },
        score = hit.score().takeIf { request.includeScores },
      )
    }
    val page = PageImpl(results, pageable, response.responses().first().result().hits().total()?.value() ?: 0)
    return ContactSearchResponse(
      page.numberOfElements,
      page.pageable.pageNumber,
      page.totalElements,
      page.totalPages,
      results,
    )
  }

  private fun emptyQuery(
    request: ContactSearchRequest,
    pageable: Pageable,
  ): ContactSearchResponse {
    val searchRequest = SearchRequest.of { req ->
      req.query(BoolQuery.of { bool -> bool.filter { it.matchesCrn(request.crn) } }.toQuery()).withPageable(pageable)
    }
    val response = openSearchClient.search(searchRequest, ContactSearchResult::class.java)
    val results = response.hits().hits().mapNotNull { hit ->
      hit.source()?.copy(score = hit.score().takeIf { request.includeScores })
    }
    val page = PageImpl(results, pageable, response.hits().total()?.value() ?: 0)
    return ContactSearchResponse(
      page.numberOfElements,
      page.pageable.pageNumber,
      page.totalElements,
      page.totalPages,
      results,
    )
  }

  private fun crnExistsInIndex(request: ContactSearchRequest): Boolean {
    val count = openSearchClient.search(
      { searchRequest ->
        searchRequest.index(INDEX_NAME)
          .routing(request.crn)
          .query { q -> q.matchesCrn(request.crn) }
          .trackTotalHits(TrackHits.of { it.count(1) })
          .size(0)
      },
      Any::class.java,
    ).hits().total()?.value() ?: 0
    return count > 0
  }

  @WithSpan
  private fun loadData(crn: String) {
    val startTime = System.currentTimeMillis()
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
    // Split request into batches of 50
    operations
      .chunked(onDemandDataloadBatchSize)
      .map { operationsChunk ->
        val request = BulkRequest.Builder()
          .index(INDEX_NAME)
          .routing(crn)
          .refresh(Refresh.True)
          .timeout { it.time("5m") }
          .operations(operationsChunk)
          .build()

        val response = openSearchClient.bulk(request)
        val errors =
          response.items().map { it.error() }.filter { it != null && it.type() != "version_conflict_engine_exception" }
        if (errors.isNotEmpty()) {
          throw RuntimeException("${errors.size} indexing errors found. First with reason: ${errors.first()?.reason()}")
        }
      }

    telemetryClient.trackEvent(
      "OnDemandDataLoad",
      mapOf("crn" to crn),
      mapOf(
        "duration" to (System.currentTimeMillis() - startTime).toDouble(),
        "count" to operations.size.toDouble(),
      ),
    )
  }

  private fun rollbackPartialLoad(crn: String) {
    openSearchClient.deleteByQuery {
      it.index(INDEX_NAME)
        .query { q -> q.matchesCrn(crn) }
        .routing(crn)
    }
  }
}
