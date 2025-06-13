package uk.gov.justice.hmpps.probationsearch.contactsearch.semantic

import com.microsoft.applicationinsights.TelemetryClient
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery
import org.opensearch.client.opensearch._types.query_dsl.ChildScoreMode
import org.opensearch.client.opensearch._types.query_dsl.HybridQuery
import org.opensearch.client.opensearch._types.query_dsl.NestedQuery
import org.opensearch.client.opensearch._types.query_dsl.Operator
import org.opensearch.client.opensearch._types.query_dsl.Query.Builder
import org.opensearch.client.opensearch.core.msearch.RequestItem
import org.opensearch.client.opensearch.core.search.HighlightField
import org.opensearch.client.opensearch.core.search.HighlighterEncoder
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.probationsearch.contactsearch.extensions.OpenSearchJavaClientExtensions.buildSortOptions
import uk.gov.justice.hmpps.probationsearch.contactsearch.extensions.OpenSearchJavaClientExtensions.hits
import uk.gov.justice.hmpps.probationsearch.contactsearch.extensions.OpenSearchJavaClientExtensions.matchesCrn
import uk.gov.justice.hmpps.probationsearch.contactsearch.extensions.OpenSearchJavaClientExtensions.msearch
import uk.gov.justice.hmpps.probationsearch.contactsearch.extensions.OpenSearchJavaClientExtensions.search
import uk.gov.justice.hmpps.probationsearch.contactsearch.extensions.OpenSearchJavaClientExtensions.withPageable
import uk.gov.justice.hmpps.probationsearch.contactsearch.extensions.TextExtensions.asHighlightedFragmentOf
import uk.gov.justice.hmpps.probationsearch.contactsearch.extensions.TextExtensions.asTextChunks
import uk.gov.justice.hmpps.probationsearch.contactsearch.model.ContactSearchRequest
import uk.gov.justice.hmpps.probationsearch.contactsearch.model.ContactSearchResponse
import uk.gov.justice.hmpps.probationsearch.contactsearch.model.ContactSearchResult
import uk.gov.justice.hmpps.probationsearch.contactsearch.semantic.dataload.ContactDataLoadService
import uk.gov.justice.hmpps.probationsearch.utils.TermSplitter
import kotlin.time.DurationUnit.MILLISECONDS
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

@Service
class ContactSemanticSearchService(
  private val openSearchClient: OpenSearchClient,
  private val dataLoadService: ContactDataLoadService,
  private val telemetryClient: TelemetryClient,
) {
  companion object {
    const val INDEX_NAME = "contact-semantic-search-primary"
    const val MIN_SCORE = 0.798F

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

  fun search(request: ContactSearchRequest, pageable: Pageable): ContactSearchResponse {
    val dataLoadDuration = measureTime { dataLoadService.loadDataOnDemand(request.crn) }
    val (response, searchDuration) = measureTimedValue { doSearch(request, pageable) }
    telemetryClient.trackEvent(
      "SemanticSearchCompleted",
      mapOf(
        "crn" to request.crn,
        "query" to request.query.length.toString(),
        "resultCount" to response.totalResults.toString(),
        "queryTermCount" to TermSplitter.split(request.query).size.toString(),
        "page" to pageable.pageNumber.toString(),
        "resultCountForPage" to response.results.size.toString(),
        "semanticOnlyResultCountForPage" to response.results.count { it.semanticMatch }.toString(),
      ),
      mapOf(
        "searchDuration" to searchDuration.toDouble(MILLISECONDS),
        "dataLoadDuration" to dataLoadDuration.toDouble(MILLISECONDS),
      ),
    )
    return response
  }

  private fun doSearch(request: ContactSearchRequest, pageable: Pageable): ContactSearchResponse {
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
              .minScore(MIN_SCORE)
              .filter(Builder().matchesCrn(request.crn).build())
          }
        }
    }.toQuery()
    val hybridQuery =
      HybridQuery.of { hybrid -> hybrid.queries(keywordQuery, semanticQuery).paginationDepth(10000) }.toQuery()

    // Using "msearch" to execute two requests in parallel - hybrid + semantic-only (for highlighting)
    val response = openSearchClient.msearch<ContactSearchResult> { msearch ->
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
    }

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
    val response = openSearchClient.search<ContactSearchResult> { searchRequest ->
      searchRequest
        .index(INDEX_NAME)
        .routing(request.crn)
        .query(BoolQuery.of { bool -> bool.filter { it.matchesCrn(request.crn) } }.toQuery())
        .withPageable(pageable)
    }
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
}