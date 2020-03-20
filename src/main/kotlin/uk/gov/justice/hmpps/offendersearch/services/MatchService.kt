package uk.gov.justice.hmpps.offendersearch.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.offendersearch.dto.MatchRequest
import uk.gov.justice.hmpps.offendersearch.dto.OffenderDetail
import uk.gov.justice.hmpps.offendersearch.dto.OffenderMatch
import uk.gov.justice.hmpps.offendersearch.dto.OffenderMatches
import java.time.LocalDate

@Service
class MatchService(
    @param:Qualifier("elasticSearchClient") private val elasticSearchClient: RestHighLevelClient,
    private val mapper: ObjectMapper
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun match(matchRequest: MatchRequest): OffenderMatches {
    val searchSourceBuilder = SearchSourceBuilder().apply {
      query(buildMatchWithAllProvidedParameters(matchRequest))
    }
    val searchRequest = SearchRequest(arrayOf("offender"), searchSourceBuilder)
    return getSearchResult(elasticSearchClient.search(searchRequest))
  }

  fun buildMatchWithAllProvidedParameters(matchRequest: MatchRequest): BoolQueryBuilder {
    with(matchRequest) {
      return QueryBuilders.boolQuery()
          .must(QueryBuilders.matchQuery("surname", surname))
          .mustWhenPresent("dateOfBirth", dateOfBirth)
          .mustWhenPresent("firstName", firstName)
          .mustWhenPresent("otherIds.croNumber", croNumber)
          .mustWhenPresent("otherIds.pncNumber", pncNumber)
          .mustWhenPresent("otherIds.nomsNumber", nomsNumber)
          .mustWhenTrue({ activeSentence }, "currentDisposal", "1")
    }
  }

  private fun getSearchResult(response: SearchResponse): OffenderMatches {
    val searchHits = response.hits.hits.asList()
    log.debug("search found ${searchHits.size} hits")
    val matchingOffenders = searchHits.map { OffenderMatch(toOffenderDetail(it.sourceAsString)) }
    return OffenderMatches(matchingOffenders)
  }

  private fun toOffenderDetail(src: String) = mapper.readValue(src, OffenderDetail::class.java)

}

fun BoolQueryBuilder.mustWhenPresent(query: String, value: String?): BoolQueryBuilder {
  value.takeIf { !it.isNullOrBlank() }?.let {
    this.must(QueryBuilders
        .matchQuery(query, it))
  }
  return this
}

fun BoolQueryBuilder.mustWhenPresent(query: String, value: LocalDate?): BoolQueryBuilder {
  value?.let {
    this.must(QueryBuilders
        .matchQuery(query, it))
  }
  return this
}

fun BoolQueryBuilder.mustWhenTrue(predicate: () -> Boolean, query: String, value: String): BoolQueryBuilder {
  value.takeIf { predicate() }?.let {
    this.must(QueryBuilders
        .matchQuery(query, it))
  }
  return this
}