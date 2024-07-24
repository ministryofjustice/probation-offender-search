package uk.gov.justice.hmpps.probationsearch.contactsearch

import com.fasterxml.jackson.databind.ObjectMapper
import io.opentelemetry.api.trace.Span
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.FieldValue
import org.opensearch.client.opensearch._types.SortOptions
import org.opensearch.client.opensearch._types.SortOrder
import org.opensearch.client.opensearch._types.query_dsl.Operator
import org.opensearch.client.opensearch.core.SearchRequest
import org.opensearch.client.opensearch.core.search.HighlightField
import org.opensearch.client.opensearch.core.search.HighlighterEncoder
import org.opensearch.client.opensearch.core.search.TrackHits
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
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
  private val auditService: HmppsAuditService?,
  private val objectMapper: ObjectMapper,
  private val deliusService: DeliusService,
  private val openSearchClient: OpenSearchClient,
) {

  private val scope = CoroutineScope(Dispatchers.IO)

  fun performSearch(request: ContactSearchRequest, pageable: Pageable): ContactSearchResponse {
    audit(request, pageable)

    val searchRequest = SearchRequest.of { searchRequest ->
      searchRequest
        .query { query ->
          query.bool { bool ->
            bool.filter { filter -> filter.match { it.field("crn").query(FieldValue.of(request.crn)) } }
            if (request.query.isNotEmpty()) bool.must { must ->
              must.simpleQueryString { simpleQueryString ->
                simpleQueryString.query(request.query)
                  .analyzeWildcard(true)
                  .defaultOperator(if (request.matchAllTerms) Operator.And else Operator.Or)
                  .fields("notes", "type", "outcome", "description")
                  .flags { it.multiple("AND|OR|PREFIX|PHRASE|PRECEDENCE|ESCAPE|FUZZY|SLOP") }
              }
            }
            bool
          }
        }
        .trackTotalHits(TrackHits.Builder().enabled(true).build())
        .size(pageable.pageSize)
        .from(pageable.offset.toInt())
        .highlight { highlight ->
          highlight
            .encoder(HighlighterEncoder.Html)
            .fields("notes", HighlightField.of { it.field("notes") })
            .fields("type", HighlightField.of { it.field("type") })
            .fields("outcome", HighlightField.of { it.field("outcome") })
            .fields("description", HighlightField.of { it.field("description") })
            .fragmentSize(200)
        }
        .sorted(pageable.sort.fieldSorts())
    }
    val searchResponse = openSearchClient.search(searchRequest, ContactSearchResult::class.java)
    val results = searchResponse.hits().hits().mapNotNull { it.source()?.copy(highlights = it.highlight()) }

    val response = PageImpl(results, pageable, searchResponse.hits().total().value())

    return ContactSearchResponse(
      response.numberOfElements,
      response.pageable.pageNumber,
      response.totalElements,
      response.totalPages,
      results,
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
    scope.launch {
      deliusService.auditContactSearch(
        ContactSearchAuditRequest(
          request,
          name,
          ContactSearchAuditRequest.PageRequest(
            pageable.pageNumber,
            pageable.pageSize,
            fieldSorts.mapNotNull { SortType.from(it.field().field())?.aliases?.first() }.joinToString(),
            fieldSorts.joinToString { it.field().order().toString() },
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

private fun Sort.Direction.toSortOrder() = when (this) {
  Sort.Direction.ASC -> SortOrder.Asc
  Sort.Direction.DESC -> SortOrder.Desc
}

private fun Sort.fieldSorts() = SortType.entries.flatMap { type ->
  type.aliases.mapNotNull { alias -> getOrderFor(alias)?.let { type.withOrder(it.direction.toSortOrder()) } }
}

private fun SortType.withOrder(order: SortOrder?) =
  SortOptions.of { options -> options.field { it.field(searchField).order(order) } }

private fun SearchRequest.Builder.sorted(sorts: List<SortOptions>): SearchRequest.Builder {
  when (sorts.size) {
    0 -> sort(SCORE.withOrder(SortOrder.Desc), LAST_UPDATED_DATETIME.withOrder(SortOrder.Desc),)
    1 -> {
      val sorted = sorts.single()
      sort(
        sorted,
        when (sorted.field().field()) {
          in SCORE.aliases -> LAST_UPDATED_DATETIME.withOrder(sorted.field().order())
          else -> SCORE.withOrder(sorted.field().order())
        },
      )
    }
    else -> sort(sorts)
  }
  return this
}
