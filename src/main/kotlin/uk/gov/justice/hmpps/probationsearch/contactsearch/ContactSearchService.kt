package uk.gov.justice.hmpps.probationsearch.contactsearch

import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.Operator
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.query.SimpleQueryStringFlag
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder
import org.elasticsearch.search.sort.SortBuilders
import org.elasticsearch.search.sort.SortOrder
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.probationsearch.contactsearch.ContactSearchService.SortType
import uk.gov.justice.hmpps.probationsearch.contactsearch.ContactSearchService.SortType.LAST_UPDATED_DATETIME
import uk.gov.justice.hmpps.probationsearch.contactsearch.ContactSearchService.SortType.SCORE

@Service
class ContactSearchService(private val es: ElasticsearchRestTemplate) {
  fun performSearch(request: ContactSearchRequest, pageable: Pageable): ContactSearchResponse {
    val query = QueryBuilders.boolQuery().fromRequest(request)

    val nsq = NativeSearchQueryBuilder()
      .withQuery(query)
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
      ).withSorts(pageable.sort)

    val searchResponse = es.search(nsq, ContactSearchResult::class.java, IndexCoordinates.of("contact-search-primary"))
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

  enum class SortType(val alias: String, val searchField: String) {
    DATE("date", "date.date"),
    LAST_UPDATED_DATETIME("lastUpdated", "lastUpdatedDateTime"),
    SCORE("relevance", "_score"),
    ;
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

private fun Sort.fieldSorts() = SortType.values().mapNotNull { type ->
  getOrderFor(type.alias)?.let { SortBuilders.fieldSort(type.searchField).order(it.direction.toSortOrder()) }
}

private fun NativeSearchQueryBuilder.withSorts(sort: Sort): NativeSearchQuery {
  val sorts = sort.fieldSorts()
  when (sorts.size) {
    0 -> {
      withSort(SortBuilders.fieldSort(SCORE.searchField).order(SortOrder.DESC))
      withSort(SortBuilders.fieldSort(LAST_UPDATED_DATETIME.searchField).order(SortOrder.DESC))
    }
    1 -> {
      val sorted = sorts.first()
      withSort(sorted)
      withSort(
        when (sorted.fieldName) {
          SCORE.alias -> SortBuilders.fieldSort(LAST_UPDATED_DATETIME.searchField).order(sorted.order())
          else -> SortBuilders.fieldSort(SCORE.searchField).order(sorted.order())
        },
      )
    }
    else -> sorts.forEach(::withSort)
  }
  return build()
}