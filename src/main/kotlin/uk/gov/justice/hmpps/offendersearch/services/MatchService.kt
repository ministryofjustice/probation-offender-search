package uk.gov.justice.hmpps.offendersearch.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.query.QueryBuilders.multiMatchQuery
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
    matchBy(matchRequest) { nomsNumber(it) } onMatch { return it.matches }
    matchBy(matchRequest) { pncNumber(it) } onMatch { return it.matches }

    // TODO remove this fallback once the full matching algorithm has been completed
    matchBy(matchRequest) { fallback(it)} onMatch { return it.matches }

    return OffenderMatches()
  }

  private fun nomsNumber(matchRequest: MatchRequest): BoolQueryBuilder? {
    return matchRequest.nomsNumber.takeIf { !it.isNullOrBlank() } ?.let {
      return QueryBuilders.boolQuery()
          .mustWhenPresent("otherIds.nomsNumber", it)
    }
  }

  private fun pncNumber(matchRequest: MatchRequest): BoolQueryBuilder? {
    return matchRequest.pncNumber.takeIf { !it.isNullOrBlank() }?.let {
      return QueryBuilders.boolQuery()
          .mustMultiMatchKeyword(it.canonicalPNCNumber(), "otherIds.pncNumberLongYear", "otherIds.pncNumberShortYear")
    }
  }

  private fun fallback(matchRequest: MatchRequest): BoolQueryBuilder? {
    return buildMatchWithAllProvidedParameters(matchRequest)
  }


  private fun matchBy(matchRequest: MatchRequest, queryBuilder: (matchRequest: MatchRequest) -> BoolQueryBuilder?): Result<Unit, OffenderMatches> {
    val matchQuery = queryBuilder(matchRequest)
    return matchQuery?.let {
      val searchSourceBuilder = SearchSourceBuilder().apply {
        query(matchQuery.withDefaults(matchRequest))
      }
      val searchRequest = SearchRequest(arrayOf("offender"), searchSourceBuilder)
      val offenderMatches = getSearchResult(elasticSearchClient.search(searchRequest))
      return if (offenderMatches.isEmpty) Result.NoMatch() else Result.Match(offenderMatches)
    } ?: Result.NoMatch()
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

fun BoolQueryBuilder.must(query: String, value: Any): BoolQueryBuilder {
  this.must(QueryBuilders.matchQuery(query, value))
  return this
}

fun BoolQueryBuilder.mustWhenTrue(predicate: () -> Boolean, query: String, value: String): BoolQueryBuilder {
  value.takeIf { predicate() }?.let {
    this.must(QueryBuilders
        .matchQuery(query, it))
  }
  return this
}

fun BoolQueryBuilder.mustMultiMatchKeyword(value: String, vararg query: String): BoolQueryBuilder {
  this.must().add(multiMatchQuery(value, *query)
      .analyzer("whitespace")
  )
  return this
}


sealed class Result<out T, out M> {
  data class NoMatch<out T>(val value: T? = null) : Result<T, Nothing>()
  data class Match<out M>(val matches: M) : Result<Nothing, M>()
}

inline infix fun <T, M> Result<T, M>.onMatch(block: (Result.Match<M>) -> Nothing): T? {
  return when (this) {
    is Result.NoMatch<T> -> value
    is Result.Match<M> -> block(this)
  }
}

private fun BoolQueryBuilder.withDefaults(matchRequest: MatchRequest): BoolQueryBuilder? {
  return this
      .mustWhenTrue({ matchRequest.activeSentence }, "currentDisposal", "1")
      .must("softDeleted", false)
}

