package uk.gov.justice.hmpps.probationsearch.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.lucene.search.join.ScoreMode
import org.opensearch.action.search.SearchRequest
import org.opensearch.action.search.SearchResponse
import org.opensearch.index.query.BoolQueryBuilder
import org.opensearch.index.query.QueryBuilders
import org.opensearch.search.builder.SearchSourceBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.probationsearch.dto.MatchRequest
import uk.gov.justice.hmpps.probationsearch.dto.MatchedBy
import uk.gov.justice.hmpps.probationsearch.dto.OffenderDetail
import uk.gov.justice.hmpps.probationsearch.dto.OffenderMatch
import uk.gov.justice.hmpps.probationsearch.dto.OffenderMatches
import java.time.DateTimeException
import java.time.LocalDate

@Service
class MatchService(
  private val openSearchClient: SearchClient,
  private val mapper: ObjectMapper,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun match(matchRequest: MatchRequest): OffenderMatches {
    matchBy(matchRequest) { fullMatch(it) } onMatch { return OffenderMatches(it.matches, MatchedBy.ALL_SUPPLIED) }
    matchBy(matchRequest) { fullMatchAlias(it) } onMatch {
      return OffenderMatches(
        it.matches,
        MatchedBy.ALL_SUPPLIED_ALIAS,
      )
    }
    matchBy(matchRequest) { nomsNumber(it) } onMatch { return OffenderMatches(it.matches, MatchedBy.HMPPS_KEY) }
    matchBy(matchRequest) { croNumber(it) } onMatch { return OffenderMatches(it.matches, MatchedBy.EXTERNAL_KEY) }
    matchBy(matchRequest) { pncNumber(it) } onMatch { return OffenderMatches(it.matches, MatchedBy.EXTERNAL_KEY) }
    matchBy(matchRequest) { nameMatch(it) } onMatch { return OffenderMatches(it.matches, MatchedBy.NAME) }
    matchBy(matchRequest) { partialNameMatch(it) } onMatch {
      return OffenderMatches(
        it.matches,
        MatchedBy.PARTIAL_NAME,
      )
    }
    matchBy(matchRequest) { partialNameMatchDateOfBirthLenient(it) } onMatch {
      return OffenderMatches(
        it.matches,
        MatchedBy.PARTIAL_NAME_DOB_LENIENT,
      )
    }
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
          .must(
            QueryBuilders.boolQuery()
              .shouldMultiMatch(surname, "surname", "offenderAliases.surname")
              .shouldMultiMatch(dateOfBirth, "dateOfBirth", "offenderAliases.dateOfBirth"),
          )
      }
    }
  }

  private fun croNumber(matchRequest: MatchRequest): BoolQueryBuilder? {
    with(matchRequest) {
      return croNumber.takeIf { !it.isNullOrBlank() }?.let {
        return QueryBuilders.boolQuery()
          .mustKeyword(it.lowercase(), "otherIds.croNumberLowercase")
          .must(
            QueryBuilders.boolQuery()
              .shouldMultiMatch(surname, "surname", "offenderAliases.surname")
              .shouldMultiMatch(dateOfBirth, "dateOfBirth", "offenderAliases.dateOfBirth"),
          )
      }
    }
  }

  private fun fullMatch(matchRequest: MatchRequest): BoolQueryBuilder {
    with(matchRequest) {
      return QueryBuilders.boolQuery()
        .mustKeyword(croNumber?.lowercase(), "otherIds.croNumberLowercase")
        .mustMultiMatchKeyword(
          pncNumber?.canonicalPNCNumber(),
          "otherIds.pncNumberLongYear",
          "otherIds.pncNumberShortYear",
        )
        .mustWhenPresent("otherIds.nomsNumber", nomsNumber)
        .mustWhenTrue({ activeSentence }, "currentDisposal", "1").apply {
          this.must(nameQuery(matchRequest))
        }
    }
  }

  private fun fullMatchAlias(matchRequest: MatchRequest): BoolQueryBuilder {
    with(matchRequest) {
      return QueryBuilders.boolQuery()
        .mustKeyword(croNumber?.lowercase(), "otherIds.croNumberLowercase")
        .mustMultiMatchKeyword(
          pncNumber?.canonicalPNCNumber(),
          "otherIds.pncNumberLongYear",
          "otherIds.pncNumberShortYear",
        )
        .mustWhenPresent("otherIds.nomsNumber", nomsNumber)
        .mustWhenTrue({ activeSentence }, "currentDisposal", "1").apply {
          this.must(aliasQuery(matchRequest))
        }
    }
  }

  private fun nameMatch(matchRequest: MatchRequest): BoolQueryBuilder? {
    with(matchRequest) {
      return QueryBuilders.boolQuery()
        .mustWhenTrue({ activeSentence }, "currentDisposal", "1")
        .must(
          QueryBuilders.boolQuery()
            .should(nameQuery(matchRequest))
            .should(aliasQuery(matchRequest)),
        )
    }
  }

  private fun nameQuery(matchRequest: MatchRequest): BoolQueryBuilder? {
    with(matchRequest) {
      return QueryBuilders.boolQuery()
        .should(
          QueryBuilders.boolQuery()
            .mustWhenPresent("surname", surname)
            .mustWhenPresent("firstName", firstName)
            .mustWhenPresent("dateOfBirth", dateOfBirth),
        )
    }
  }

  private fun aliasQuery(matchRequest: MatchRequest): BoolQueryBuilder? {
    with(matchRequest) {
      return QueryBuilders.boolQuery()
        .should(
          QueryBuilders.nestedQuery(
            "offenderAliases",
            QueryBuilders.boolQuery()
              .mustWhenPresent("offenderAliases.surname", surname)
              .mustWhenPresent("offenderAliases.firstName", firstName)
              .mustWhenPresent("offenderAliases.dateOfBirth", dateOfBirth),
            ScoreMode.Max,
          ),
        )
    }
  }

  private fun partialNameMatch(matchRequest: MatchRequest): BoolQueryBuilder {
    with(matchRequest) {
      return QueryBuilders.boolQuery()
        .mustWhenTrue({ activeSentence }, "currentDisposal", "1")
        .mustWhenPresent("surname", surname)
        .mustWhenPresent("dateOfBirth", dateOfBirth)
    }
  }

  private fun partialNameMatchDateOfBirthLenient(matchRequest: MatchRequest): BoolQueryBuilder? {
    with(matchRequest) {
      return dateOfBirth?.let {
        QueryBuilders.boolQuery()
          .mustWhenTrue({ activeSentence }, "currentDisposal", "1")
          .mustMultiMatch(firstName, "firstName", "offenderAliases.firstName")
          .mustWhenPresent("surname", surname)
          .mustMatchOneOf("dateOfBirth", allLenientDateVariations(dateOfBirth))
      }
    }
  }

  private fun allLenientDateVariations(date: LocalDate): List<LocalDate> {
    return swapMonthDay(date) + everyOtherValidMonth(date) + aroundDateInSameMonth(date)
  }

  private fun aroundDateInSameMonth(date: LocalDate) =
    listOf(date.minusDays(1), date.minusDays(-1), date).filter { it.month == date.month }

  private fun everyOtherValidMonth(date: LocalDate): List<LocalDate> =
    (1..12).filterNot { date.monthValue == it }.mapNotNull { setMonthDay(date, it) }

  private fun swapMonthDay(date: LocalDate): List<LocalDate> = try {
    listOf(LocalDate.of(date.year, date.dayOfMonth, date.monthValue))
  } catch (e: DateTimeException) {
    listOf()
  }

  private fun setMonthDay(date: LocalDate, monthValue: Int): LocalDate? = try {
    LocalDate.of(date.year, monthValue, date.dayOfMonth)
  } catch (e: DateTimeException) {
    null
  }

  private fun matchBy(
    matchRequest: MatchRequest,
    queryBuilder: (matchRequest: MatchRequest) -> BoolQueryBuilder?,
  ): Result {
    val matchQuery = queryBuilder(matchRequest)
    return matchQuery?.let {
      val searchSourceBuilder = SearchSourceBuilder().apply {
        query(matchQuery.withDefaults(matchRequest))
      }
      val searchRequest = SearchRequest(arrayOf("person-search-primary"), searchSourceBuilder)
      val offenderMatches = getSearchResult(openSearchClient.search(searchRequest))
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

inline infix fun Result.onMatch(block: (Result.Match) -> Nothing) {
  return when (this) {
    is Result.NoMatch -> {
    }
    is Result.Match -> block(this)
  }
}

private fun BoolQueryBuilder.withDefaults(matchRequest: MatchRequest): BoolQueryBuilder {
  return this
    .mustWhenTrue({ matchRequest.activeSentence }, "currentDisposal", "1")
    .must("softDeleted", false)
}
