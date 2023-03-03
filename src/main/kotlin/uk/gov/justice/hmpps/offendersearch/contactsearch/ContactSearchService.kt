package uk.gov.justice.hmpps.offendersearch.contactsearch

import org.elasticsearch.index.query.Operator
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.query.SimpleQueryStringFlag
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder
import org.elasticsearch.search.sort.SortBuilders
import org.elasticsearch.search.sort.SortOrder
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.stereotype.Service

@Service
class ContactSearchService(private val es: ElasticsearchRestTemplate) {
  fun performSearch(request: ContactSearchRequest, pageable: Pageable): SearchContactResponse {
    // Filter on CRN, then search on contact fields if the query is non-empty
    val query = QueryBuilders.boolQuery().filter(QueryBuilders.matchQuery("crn", request.crn))
    if (request.query.isNotEmpty()) {
      query.must(
        QueryBuilders.simpleQueryStringQuery(request.query)
          .analyzeWildcard(true)
          .defaultOperator(if (request.matchAllTerms) Operator.AND else Operator.OR)
          .field("notes")
          .field("contact_type")
          .field("contact_outcome")
          .field("contact_description")
          .flags(
            SimpleQueryStringFlag.AND,
            SimpleQueryStringFlag.OR,
            SimpleQueryStringFlag.PREFIX,
            SimpleQueryStringFlag.PHRASE,
            SimpleQueryStringFlag.PRECEDENCE,
            SimpleQueryStringFlag.ESCAPE,
            SimpleQueryStringFlag.FUZZY,
            SimpleQueryStringFlag.SLOP
          )
      )
    }

    var searchResponseBuilder = NativeSearchQueryBuilder()
      .withQuery(query)
      .withPageable(pageable)
      .withTrackTotalHits(true)
      .withHighlightBuilder(
        HighlightBuilder()
          .encoder("html")
          .field("notes")
          .field("contact_type")
          .field("contact_outcome")
          .field("contact_description")
          .fragmentSize(200)
      )

    val sortOrder = if (request.sortDirection == SortDirection.ASCENDING) SortOrder.ASC else SortOrder.DESC
    searchResponseBuilder = when (request.sort) {
      SortField.LAST_UPDATED_DATETIME -> {
        searchResponseBuilder.withSort(SortBuilders.fieldSort("last_updated_date_time").order(sortOrder))
        searchResponseBuilder.withSort(SortBuilders.fieldSort("_score").order(sortOrder))
      }
      SortField.CONTACT_DATE -> {
        searchResponseBuilder.withSort(SortBuilders.fieldSort("contact_date.date").order(sortOrder))
        searchResponseBuilder.withSort(SortBuilders.fieldSort("_score").order(sortOrder))
      }
      else -> {
        searchResponseBuilder.withSort(SortBuilders.fieldSort("_score").order(sortOrder))
        searchResponseBuilder.withSort(SortBuilders.fieldSort("last_updated_date_time").order(sortOrder))
      }
    }

    val searchResponse = es.search(searchResponseBuilder.build(), Contact::class.java)

    val results = searchResponse.searchHits.mapNotNull { it.content }

    val response = PageImpl(results, pageable, searchResponse.totalHits)
    return SearchContactResponse(
      response.numberOfElements,
      response.pageable.pageNumber,
      response.totalElements,
      response.totalPages,
      results
    )
  }
}
