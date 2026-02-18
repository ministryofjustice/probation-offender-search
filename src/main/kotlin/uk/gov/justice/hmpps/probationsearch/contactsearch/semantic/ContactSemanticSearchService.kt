package uk.gov.justice.hmpps.probationsearch.contactsearch.semantic

import com.microsoft.applicationinsights.TelemetryClient
import org.opensearch.client.json.JsonData
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.query_dsl.*
import org.opensearch.client.opensearch.core.search.HighlightField
import org.opensearch.client.opensearch.core.search.HighlighterEncoder
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.probationsearch.contactsearch.extensions.OpenSearchJavaClientExtensions.matchesCrn
import uk.gov.justice.hmpps.probationsearch.contactsearch.extensions.OpenSearchJavaClientExtensions.search
import uk.gov.justice.hmpps.probationsearch.contactsearch.extensions.OpenSearchJavaClientExtensions.term
import uk.gov.justice.hmpps.probationsearch.contactsearch.extensions.OpenSearchJavaClientExtensions.withPageable
import uk.gov.justice.hmpps.probationsearch.contactsearch.extensions.TextExtensions.asHighlightedFragmentOf
import uk.gov.justice.hmpps.probationsearch.contactsearch.extensions.TextExtensions.asTextChunks
import uk.gov.justice.hmpps.probationsearch.contactsearch.model.ContactSearchRequest
import uk.gov.justice.hmpps.probationsearch.contactsearch.model.ContactSearchResponse
import uk.gov.justice.hmpps.probationsearch.contactsearch.model.ContactSearchResult
import uk.gov.justice.hmpps.probationsearch.contactsearch.semantic.dataload.ContactDataLoadService
import uk.gov.justice.hmpps.probationsearch.utils.Retry.retry
import uk.gov.justice.hmpps.probationsearch.utils.TermSplitter
import kotlin.time.DurationUnit.MILLISECONDS
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

    enum class ContactFilter(val filterName: String, val queries: List<Query>) {
      COMPLIED(
        "complied",
        listOf(
          Query.of { query ->
            query.match { match ->
              match.field("complied").query { it.stringValue("complied") }
            }
          },
        ),
      ),
      NOT_COMPLIED(
        "notComplied",
        listOf(Query.of { query -> query.match { match -> match.field("complied").query { it.stringValue("ftc") } } }),
      ),
      NO_OUTCOME(
        "noOutcome",
        listOf(
          BoolQuery.of { bool ->
            bool.must(
              listOf(
                Query.of { query -> query.term("requiresOutcome" to "Y") },
                Query.of { query -> query.term("outcomeRequiredFlag" to "Y") },
              ),
            )
          }.toQuery(),
        ),
      ),
    }

    val KEYWORD_SEARCH_FIELDS = listOf(
      "notes",
      "description",
      "typeCode",
      "typeDescription",
      "outcomeCode",
      "outcomeDescription",
      "attended",
      "complied",
      "date",
    )
    val HIGHLIGHT_FIELDS = listOf(
      "notes",
      "description",
      "typeCode",
      "typeDescription",
      "outcomeCode",
      "outcomeDescription",
      "date",
    )
    val RETURN_FIELDS = listOf(
      "crn",
      "id",
      "notes",
      "description",
      "typeCode",
      "typeDescription",
      "outcomeCode",
      "outcomeDescription",
      "date",
      "startTime",
      "endTime",
      "lastUpdatedDateTime",
    )
  }

  fun search(request: ContactSearchRequest, pageable: Pageable): ContactSearchResponse {
    val (dataLoadCount, dataLoadDuration) = measureTimedValue { dataLoadService.loadDataOnDemand(request.crn) }
    val (response, searchDuration) = measureTimedValue { doSearch(request, pageable) }
    telemetryClient.trackEvent(
      "SemanticSearchCompleted",
      mapOf(
        "crn" to request.crn,
        "query" to request.query.length.toString(),
        "resultCount" to response.totalResults.toString(),
        "requiredDataLoad" to (dataLoadCount != null).toString(),
        "dataLoadCount" to dataLoadCount?.toString(),
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
    if (request.query.isBlank()) return emptyQuery(request, pageable)

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
          query.neural { neural ->
            neural
              .field("textEmbedding.knn")
              .queryText(request.query)
              .minScore(MIN_SCORE)
              .filter { it.term("textEmbedding.crn" to request.crn) }
          }
        }
    }.toQuery()
    val hybridQuery =
      HybridQuery.of { hybrid -> hybrid.queries(keywordQuery, semanticQuery).paginationDepth(10000) }.toQuery()

    val filters = buildFilters(request)

    val response = retry {
      openSearchClient.search<ContactSearchResult> { search ->
        search.index(INDEX_NAME)
          .query(hybridQuery)
          .postFilter(BoolQuery.of { bool -> bool.filter(filters) }.toQuery())
          .source { source -> source.filter { it.includes(RETURN_FIELDS) } }
          .withPageable(pageable)
          .highlight { highlight ->
            highlight
              .encoder(HighlighterEncoder.Html)
              .fields(HIGHLIGHT_FIELDS.associateWith { HighlightField.of { it } })
              .fragmentSize(200)
          }
      }
    }

    val results = response.hits().hits().mapNotNull { hit ->
      hit.source()?.copy(
        semanticMatch = hit.highlight().isEmpty(),
        highlights = hit.highlight().ifEmpty {
          // If no highlight from OpenSearch, check if we have inner hits for the document
          hit.source()?.notes?.let { notes ->
            hit.innerHits()["textEmbedding"]?.hits()?.hits()?.mapNotNull { it.nested()?.offset() }
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
    val page = PageImpl(results, pageable, response.hits().total()?.value() ?: 0)
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
    val filters = buildFilters(request)
    val crnAndFilters = listOf(Query.of { it.matchesCrn(request.crn) }) + filters

    val response = retry {
      openSearchClient.search<ContactSearchResult> { searchRequest ->
        searchRequest
          .index(INDEX_NAME)
          .pipeline("_none")
          .query(BoolQuery.of { bool -> bool.filter(crnAndFilters) }.toQuery())
          .withPageable(pageable)
      }
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

  private fun buildFilters(request: ContactSearchRequest): List<Query> {
    val filters = mutableListOf<Query>()

    request.dateFrom?.let { dateFrom ->
      filters.add(
        Query.of { query ->
          query.range { dateRange ->
            dateRange.field("date.date").gte(JsonData.of(dateFrom.toString()))
          }
        },
      )
    }

    request.dateTo?.let { dateTo ->
      filters.add(
        Query.of { query ->
          query.range { dateRange ->
            dateRange.field("date.date").lte(JsonData.of(dateTo.toString()))
          }
        },
      )
    }

    if (!request.includeSystemGenerated) {
      filters.add(Query.of { query -> query.term("systemGenerated" to "N") })
    }

    val contactFilters = ContactFilter.entries.filter { request.filters.contains(it.filterName) }
    if (contactFilters.isNotEmpty()) {
      val shouldQueries = contactFilters.flatMap { it.queries }
      filters.add(BoolQuery.of { bool -> bool.should(shouldQueries).minimumShouldMatch("1") }.toQuery())
    }

    return filters
  }
}