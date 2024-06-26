package uk.gov.justice.hmpps.probationsearch.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.lucene.search.join.ScoreMode
import org.opensearch.action.search.SearchRequest
import org.opensearch.action.search.SearchResponse
import org.opensearch.index.query.BoolQueryBuilder
import org.opensearch.index.query.QueryBuilders
import org.opensearch.search.builder.SearchSourceBuilder
import org.opensearch.search.sort.SortOrder
import org.opensearch.search.suggest.SuggestBuilder
import org.opensearch.search.suggest.term.TermSuggestionBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.probationsearch.dto.*
import java.time.LocalDate
import java.util.*

@Service
class SearchService @Autowired constructor(
  private val offenderAccessService: OffenderAccessService,
  private val hlClient: SearchClient,
  private val mapper: ObjectMapper,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
    private const val MAX_SEARCH_RESULTS = 100
  }

  fun performSearch(searchOptions: SearchDto): List<OffenderDetail> {
    validateSearchForm(searchOptions)
    val searchRequest = personSearchRequest()
    val searchSourceBuilder = SearchSourceBuilder()
    // Set the maximum search result size (the default would otherwise be 10)
    searchSourceBuilder.size(MAX_SEARCH_RESULTS)
    val matchingAllFieldsQuery = buildMatchWithAllProvidedParameters(searchOptions)
    searchSourceBuilder.query(matchingAllFieldsQuery.withDefaults())
    searchRequest.source(searchSourceBuilder)
    val response = hlClient.search(searchRequest)
    return getSearchResult(response)
  }

  fun performPagedSearch(pageable: Pageable, searchOptions: SearchDto): SearchPagedResults {
    validateSearchForm(searchOptions)
    val searchSourceBuilder = SearchSourceBuilder()
      .size(pageable.pageSize)
      .from(pageable.offset.toInt())
      .query(buildMatchWithAllProvidedParameters(searchOptions).withDefaults())

    val response = hlClient.search(personSearchRequest().source(searchSourceBuilder))
    return SearchPagedResults(
      content = PageImpl(getSearchResult(response), pageable, response.hits.totalHits?.value ?: 0),
      pageable = pageable,
      total = response.hits.totalHits?.value ?: 0,
    )
  }

  protected fun buildMatchWithAllProvidedParameters(searchOptions: SearchDto): BoolQueryBuilder {
    val matchingAllFieldsQuery = QueryBuilders
      .boolQuery()
    with(searchOptions) {
      croNumber.takeIf { !it.isNullOrBlank() }?.let {
        matchingAllFieldsQuery
          .mustKeyword(it.lowercase(Locale.getDefault()), "otherIds.croNumberLowercase")
      }
      pncNumber.takeIf { !it.isNullOrBlank() }?.let {
        matchingAllFieldsQuery
          .mustMultiMatchKeyword(it.canonicalPNCNumber(), "otherIds.pncNumberLongYear", "otherIds.pncNumberShortYear")
      }
      matchingAllFieldsQuery
        .mustWhenPresent("otherIds.nomsNumber", nomsNumber)
      if (!crn.isNullOrBlank()) {
        matchingAllFieldsQuery.atLeastOneMatches(listOf("otherIds.crn", "otherIds.previousCrn"), crn)
      }

      matchesPersonOrAlias(matchingAllFieldsQuery)
    }

    return matchingAllFieldsQuery
  }

  private fun SearchDto.matchesPersonOrAlias(matchingAllFieldsQuery: BoolQueryBuilder) {
    PersonAliasSearch.from(firstName, surname, dateOfBirth)?.run {
      val matchPerson = QueryBuilders.boolQuery().minimumShouldMatch(numberOfFieldsToMatch())
      val matchAlias = QueryBuilders.boolQuery().minimumShouldMatch(numberOfFieldsToMatch())
      firstName?.let {
        matchPerson.should(QueryBuilders.matchQuery("firstName", it))
        matchAlias.should(QueryBuilders.matchQuery("offenderAliases.firstName", it))
      }
      surname?.let {
        matchPerson.should(QueryBuilders.matchQuery("surname", it))
        matchAlias.should(QueryBuilders.matchQuery("offenderAliases.surname", it))
      }
      dob?.let {
        matchPerson.should(QueryBuilders.matchQuery("dateOfBirth", it))
        matchAlias.should(QueryBuilders.matchQuery("offenderAliases.dateOfBirth", it))
      }

      val matchPersonOrAlias = QueryBuilders.boolQuery().minimumShouldMatch(1)
      matchPersonOrAlias.should(matchPerson)
      if (includeAliases == true) {
        matchPersonOrAlias.should(QueryBuilders.nestedQuery("offenderAliases", matchAlias, ScoreMode.Max))
      }

      matchingAllFieldsQuery.must(matchPersonOrAlias)
    }
  }

  private fun validateSearchForm(searchOptions: SearchDto) {
    if (!searchOptions.isValid) {
      throw uk.gov.justice.hmpps.probationsearch.BadRequestException("Invalid search  - please provide at least 1 search parameter")
    }
  }

  private fun getSearchResult(response: SearchResponse): List<OffenderDetail> = response.hits.hits.map {
    parseOffenderDetail(it.sourceAsString)
  }

  private fun parseOffenderDetail(src: String): OffenderDetail {
    return try {
      mapper.readValue(src, OffenderDetail::class.java)
    } catch (t: Throwable) {
      throw RuntimeException(t)
    }
  }

  private fun BoolQueryBuilder.withDefaults(): BoolQueryBuilder {
    return this
      .must("softDeleted", false)
  }

  fun performSearch(
    searchPhraseFilter: SearchPhraseFilter,
    pageable: Pageable,
    offenderUserAccess: OffenderUserAccess,
  ): SearchPhraseResults {
    fun canAccessOffender(offenderDetail: OffenderDetail): Boolean {
      return offenderAccessService.canAccessOffender(offenderDetail, offenderUserAccess)
    }

    val searchRequest = personSearchRequest()
      .source(
        SearchSourceBuilder()
          .query(buildQuery(searchPhraseFilter.phrase, searchPhraseFilter.matchAllTerms))
          .size(pageable.pageSize)
          .from(pageable.offset.toInt())
          .sort("_score")
          .sort("offenderId", SortOrder.DESC)
          .trackTotalHits(true)
          .aggregation(buildAggregationRequest())
          .highlighter(buildHighlightRequest())
          .suggest(
            SuggestBuilder()
              .addSuggestion("surname", TermSuggestionBuilder("surname").text(searchPhraseFilter.phrase))
              .addSuggestion("firstName", TermSuggestionBuilder("firstName").text(searchPhraseFilter.phrase)),
          )
          .apply {
            buildProbationAreaFilter(searchPhraseFilter.probationAreasFilter)?.run { postFilter(this) }
          },
      )
    val response = hlClient.search(searchRequest)
    return SearchPhraseResults(
      pageable = pageable,
      content = PageImpl(
        extractOffenderDetailList(
          hits = response.hits.hits,
          phrase = searchPhraseFilter.phrase,
          offenderParser = ::parseOffenderDetail,
          accessChecker = ::canAccessOffender,
        ),
        pageable, response.hits.totalHits?.value ?: 0,
      ),
      total = response.hits.totalHits?.value ?: 0,
      probationAreaAggregations = extractProbationAreaAggregation(response.aggregations),
      suggestions = response.suggest,
    )
  }

  fun findByListOfCRNs(crnList: List<String>): List<OffenderDetail> {
    return findByCrn(crnList, crnList.size)
  }

  fun findByListOfNoms(nomsList: List<String>): List<OffenderDetail> {
    return findBy(nomsList, "otherIds.nomsNumber", nomsList.size)
  }

  fun findByListOfLdu(pageable: Pageable, lduList: List<String>): SearchPagedResults {
    return findByNestedList(lduList, pageable, "offenderManagers.team.localDeliveryUnit.code")
  }

  fun findByListOfTeamCodes(pageable: Pageable, teamCodeList: List<String>): SearchPagedResults {
    return findByNestedList(teamCodeList, pageable, "offenderManagers.team.code")
  }

  fun findByLduCode(lduCode: String, pageable: Pageable): SearchPagedResults {
    return findByNested(lduCode, pageable, "offenderManagers.team.localDeliveryUnit.code")
  }

  fun findByTeamCode(teamCode: String, pageable: Pageable): SearchPagedResults {
    return findByNested(teamCode, pageable, "offenderManagers.team.code")
  }

  fun findBy(inputList: List<String>, field: String, searchSourceBuilderSize: Int): List<OffenderDetail> {
    val searchRequest = personSearchRequest()
    val searchSourceBuilder = SearchSourceBuilder()

    searchSourceBuilder.size(searchSourceBuilderSize)

    val outerMustQuery = QueryBuilders.boolQuery()
    val matchingAllFieldsQuery = QueryBuilders.boolQuery()
    inputList.forEach {
      matchingAllFieldsQuery
        .should(QueryBuilders.matchQuery(field, it))
    }
    outerMustQuery.must(matchingAllFieldsQuery)
    searchSourceBuilder.query(outerMustQuery.withDefaults())
    searchRequest.source(searchSourceBuilder)
    val response = hlClient.search(searchRequest)
    return getSearchResult(response)
  }

  fun findByCrn(inputList: List<String>, searchSourceBuilderSize: Int): List<OffenderDetail> {
    val searchRequest = personSearchRequest()
    val searchSourceBuilder = SearchSourceBuilder()

    searchSourceBuilder.size(searchSourceBuilderSize)

    val outerMustQuery = QueryBuilders.boolQuery()
    val matchingAllFieldsQuery = QueryBuilders.boolQuery()
    inputList.forEach {
      matchingAllFieldsQuery
        .should(QueryBuilders.matchQuery("otherIds.crn", it))
        .should(QueryBuilders.matchQuery("otherIds.previousCrn", it))
    }
    outerMustQuery.must(matchingAllFieldsQuery)
    searchSourceBuilder.query(outerMustQuery.withDefaults())
    searchRequest.source(searchSourceBuilder)
    val response = hlClient.search(searchRequest)
    return getSearchResult(response)
  }

  private fun findByNested(code: String, pageable: Pageable, searchField: String): SearchPagedResults {
    val searchRequest = personSearchRequest()
    val searchSourceBuilder = SearchSourceBuilder()
    searchSourceBuilder.size(pageable.pageSize).from(pageable.offset.toInt())

    val matchingAllFieldsQuery = QueryBuilders.boolQuery()
    val outerMustQuery = QueryBuilders.boolQuery()

    matchingFieldsQuery(matchingAllFieldsQuery, searchField, code)

    outerMustQuery.must(matchingAllFieldsQuery)
    searchSourceBuilder.query(outerMustQuery.withDefaults())
    searchRequest.source(searchSourceBuilder)
    val response = hlClient.search(searchRequest)

    return SearchPagedResults(
      content = PageImpl(getSearchResult(response), pageable, response.hits.totalHits?.value ?: 0),
      pageable = pageable,
      total = response.hits.totalHits?.value ?: 0,
    )
  }

  private fun findByNestedList(inputList: List<String>, pageable: Pageable, searchField: String): SearchPagedResults {
    val searchRequest = personSearchRequest()
    val searchSourceBuilder = SearchSourceBuilder()
    searchSourceBuilder.size(pageable.pageSize).from(pageable.offset.toInt())

    val matchingAllFieldsQuery = QueryBuilders.boolQuery()
    val outerMustQuery = QueryBuilders.boolQuery()

    inputList.forEach {
      matchingFieldsQuery(matchingAllFieldsQuery, searchField, it)
    }

    outerMustQuery.must(matchingAllFieldsQuery)
    searchSourceBuilder.query(outerMustQuery.withDefaults())
    searchRequest.source(searchSourceBuilder)
    val response = hlClient.search(searchRequest)

    return SearchPagedResults(
      content = PageImpl(getSearchResult(response), pageable, response.hits.totalHits?.value ?: 0),
      pageable = pageable,
      total = response.hits.totalHits?.value ?: 0,
    )
  }

  private fun matchingFieldsQuery(matchingAllFieldsQuery: BoolQueryBuilder, searchField: String, code: String) {
    matchingAllFieldsQuery.should(
      QueryBuilders.nestedQuery(
        "offenderManagers",
        QueryBuilders.boolQuery()
          .mustWhenPresent("offenderManagers.active", true)
          .mustWhenPresent("offenderManagers.softDeleted", false)
          .mustWhenPresent(searchField, code),
        ScoreMode.Max,
      ),
    )
  }
}

private fun personSearchRequest() = SearchRequest("person-search-primary")

data class PersonAliasSearch(val firstName: String?, val surname: String?, val dob: LocalDate?) {

  fun numberOfFieldsToMatch() = listOfNotNull(blankToNull(firstName), blankToNull(surname), dob).count()

  companion object {
    fun from(firstName: String?, surname: String?, dob: LocalDate?) =
      if (firstName.isNullOrBlank() && surname.isNullOrBlank() && dob == null) {
        null
      } else {
        PersonAliasSearch(blankToNull(firstName), blankToNull(surname), dob)
      }

    private fun blankToNull(value: String?) = if (value?.isBlank() != false) null else value
  }
}
