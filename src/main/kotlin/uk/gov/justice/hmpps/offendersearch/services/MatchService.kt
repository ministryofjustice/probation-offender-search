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
import uk.gov.justice.hmpps.offendersearch.dto.MatchedBy.ALL_SUPPLIED
import uk.gov.justice.hmpps.offendersearch.dto.MatchedBy.EXTERNAL_KEY
import uk.gov.justice.hmpps.offendersearch.dto.MatchedBy.HMPPS_KEY
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
    matchBy(matchRequest) { fullMatch(it) } onMatch { return OffenderMatches(it.matches, ALL_SUPPLIED) }
    matchBy(matchRequest) { nomsNumber(it) } onMatch { return OffenderMatches(it.matches, HMPPS_KEY) }
    matchBy(matchRequest) { croNumber(it) } onMatch { return OffenderMatches(it.matches, EXTERNAL_KEY) }
    matchBy(matchRequest) { pncNumber(it) } onMatch { return OffenderMatches(it.matches, EXTERNAL_KEY) }
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

  private fun fullMatch(matchRequest: MatchRequest): BoolQueryBuilder? {
    with(matchRequest) {
      return QueryBuilders.boolQuery()
          .mustMultiMatch(surname, "surname", "offenderAliases.surname")
          .mustMultiMatch(dateOfBirth, "dateOfBirth", "offenderAliases.dateOfBirth")
          .mustMultiMatch(firstName, "firstName", "offenderAliases.firstName")
          .mustKeyword(croNumber?.toLowerCase(), "otherIds.croNumberLowercase")
          .mustMultiMatchKeyword(pncNumber?.canonicalPNCNumber(), "otherIds.pncNumberLongYear", "otherIds.pncNumberShortYear")
          .mustWhenPresent("otherIds.nomsNumber", nomsNumber)
          .mustWhenTrue({ activeSentence }, "currentDisposal", "1")
    }
  }


  private fun matchBy(matchRequest: MatchRequest, queryBuilder: (matchRequest: MatchRequest) -> BoolQueryBuilder?): Result {
    val matchQuery = queryBuilder(matchRequest)
    return matchQuery?.let {
      val searchSourceBuilder = SearchSourceBuilder().apply {
        query(matchQuery.withDefaults(matchRequest))
      }
      val searchRequest = SearchRequest(arrayOf("offender"), searchSourceBuilder)
      val offenderMatches = getSearchResult(elasticSearchClient.search(searchRequest))
      return if (offenderMatches.isEmpty()) Result.NoMatch else Result.Match(offenderMatches)
    } ?: Result.NoMatch
  }

  private fun getSearchResult(response: SearchResponse): List<OffenderMatch> {
    val searchHits = response.hits.hits.asList()
    log.debug("search found ${searchHits.size} hits")
    return searchHits.map { OffenderMatch(toOffenderDetail(it.sourceAsString)) }
  }

  private fun toOffenderDetail(src: String) = mapper.readValue(src, OffenderDetail::class.java)

}


sealed class Result {
  object NoMatch : Result()
  data class Match(val matches: List<OffenderMatch>) : Result()
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

