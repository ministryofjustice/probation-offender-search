package uk.gov.justice.hmpps.probationsearch.contactsearch.keyword

import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder
import org.opensearch.data.client.orhlc.OpenSearchRestTemplate
import org.opensearch.index.query.BoolQueryBuilder
import org.opensearch.index.query.Operator
import org.opensearch.index.query.QueryBuilders.boolQuery
import org.opensearch.index.query.QueryBuilders.matchQuery
import org.opensearch.index.query.QueryBuilders.simpleQueryStringQuery
import org.opensearch.index.query.SimpleQueryStringFlag
import org.opensearch.search.fetch.subphase.highlight.HighlightBuilder
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.probationsearch.contactsearch.extensions.OpenSearchRestClientExtensions.fieldSorts
import uk.gov.justice.hmpps.probationsearch.contactsearch.extensions.OpenSearchRestClientExtensions.sorted
import uk.gov.justice.hmpps.probationsearch.contactsearch.model.ContactSearchRequest
import uk.gov.justice.hmpps.probationsearch.contactsearch.model.ContactSearchResponse
import uk.gov.justice.hmpps.probationsearch.contactsearch.model.ContactSearchResult

@Service
class ContactKeywordSearchService(private val restTemplate: OpenSearchRestTemplate) {
  companion object {
    const val INDEX_NAME = "contact-search-primary"
  }

  fun search(request: ContactSearchRequest, pageable: Pageable): ContactSearchResponse {
    val keywordQuery = keywordQueryForRestClient(pageable, request)
    val searchResponse =
      restTemplate.search(keywordQuery, ContactSearchResult::class.java, IndexCoordinates.of(INDEX_NAME))
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
}
