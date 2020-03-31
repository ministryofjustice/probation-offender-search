package uk.gov.justice.hmpps.offendersearch.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.query.QueryBuilders.matchQuery
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
    matchBy(matchRequest) { croNumber(it) } onMatch { return it.matches }
    matchBy(matchRequest) { pncNumber(it) } onMatch { return it.matches }

    // TODO remove this fallback once the full matching algorithm has been completed
    matchBy(matchRequest) { fallback(it) } onMatch { return it.matches }

    return OffenderMatches()
  }

  private fun nomsNumber(matchRequest: MatchRequest): BoolQueryBuilder? {
    return matchRequest.nomsNumber.takeIf { !it.isNullOrBlank() }?.let {
      // NOMS number is a special case since a human has already matched so trust that judgement
      return QueryBuilders.boolQuery()
          .mustWhenPresent("otherIds.nomsNumber", it)
    }
  }

  private fun pncNumber(matchRequest: MatchRequest): BoolQueryBuilder? {
    with(matchRequest) {
      return pncNumber.takeIf { !it.isNullOrBlank() }?.let {
        return QueryBuilders.boolQuery()
            .mustMultiMatchKeyword(it.canonicalPNCNumber(), "otherIds.pncNumberLongYear", "otherIds.pncNumberShortYear")
            .must(QueryBuilders.boolQuery()
                .shouldMultiMatch(surname, "surname", "offenderAliases.surname")
                .shouldMultiMatch(dateOfBirth, "dateOfBirth", "offenderAliases.dateOfBirth"))
      }
    }
  }

  private fun croNumber(matchRequest: MatchRequest): BoolQueryBuilder? {
    with(matchRequest) {
      return croNumber.takeIf { !it.isNullOrBlank() }?.let {
        return QueryBuilders.boolQuery()
            .mustKeyword(it.toLowerCase(), "otherIds.croNumberLowercase")
            .must(QueryBuilders.boolQuery()
                .shouldMultiMatch(surname, "surname", "offenderAliases.surname")
                .shouldMultiMatch(dateOfBirth, "dateOfBirth", "offenderAliases.dateOfBirth"))
      }
    }
  }

  private fun fallback(matchRequest: MatchRequest): BoolQueryBuilder? {
    return buildMatchWithAllProvidedParameters(matchRequest)
  }


  private fun matchBy(matchRequest: MatchRequest, queryBuilder: (matchRequest: MatchRequest) -> BoolQueryBuilder?): Result {
    val matchQuery = queryBuilder(matchRequest)
    return matchQuery?.let {
      val searchSourceBuilder = SearchSourceBuilder().apply {
        query(matchQuery.withDefaults(matchRequest))
      }
      val searchRequest = SearchRequest(arrayOf("offender"), searchSourceBuilder)
      val offenderMatches = getSearchResult(elasticSearchClient.search(searchRequest))
      return if (offenderMatches.isEmpty) Result.NoMatch else Result.Match(offenderMatches)
    } ?: Result.NoMatch
  }

  fun buildMatchWithAllProvidedParameters(matchRequest: MatchRequest): BoolQueryBuilder {
    with(matchRequest) {
      return QueryBuilders.boolQuery()
          .must(matchQuery("surname", surname))
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

fun BoolQueryBuilder.mustWhenPresent(query: String, value: Any?): BoolQueryBuilder {
  value.takeIf {
    when(it) {
      is String -> it.isNotBlank()
      else -> true
    }
  }?.let {
    this.must(matchQuery(query, it))
  }
  return this
}

fun BoolQueryBuilder.shouldMultiMatch(value: Any?, vararg query: String): BoolQueryBuilder {
  value.takeIf {
    when(it) {
      is String -> it.isNotBlank()
      else -> true
    }
  }?.let {
    this.should().add(multiMatchQuery(value, *query))
  }
  return this
}

fun BoolQueryBuilder.must(query: String, value: Any): BoolQueryBuilder {
  this.must(matchQuery(query, value))
  return this
}

fun BoolQueryBuilder.mustWhenTrue(predicate: () -> Boolean, query: String, value: String): BoolQueryBuilder {
  value.takeIf { predicate() }?.let {
    this.must(matchQuery(query, it))
  }
  return this
}

fun BoolQueryBuilder.mustMultiMatchKeyword(value: String, vararg query: String): BoolQueryBuilder {
  this.must().add(multiMatchQuery(value, *query)
      .analyzer("keyword")
  )
  return this
}


fun BoolQueryBuilder.mustKeyword(value: String, query: String): BoolQueryBuilder {
  return this.must(matchQuery(query, value).analyzer("keyword"))
}


sealed class Result {
  object NoMatch : Result()
  data class Match(val matches: OffenderMatches) : Result()
}

inline infix fun Result.onMatch(block: (Result.Match) -> Nothing): Unit? {
  return when (this) {
    is Result.NoMatch -> {
    }
    is Result.Match -> block(this)
  }
}

private fun BoolQueryBuilder.withDefaults(matchRequest: MatchRequest): BoolQueryBuilder? {
  return this
      .mustWhenTrue({ matchRequest.activeSentence }, "currentDisposal", "1")
      .must("softDeleted", false)
}

