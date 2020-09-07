package uk.gov.justice.hmpps.offendersearch.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.sort.SortOrder.DESC
import org.elasticsearch.search.suggest.SuggestBuilder
import org.elasticsearch.search.suggest.term.TermSuggestionBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.offendersearch.BadRequestException
import uk.gov.justice.hmpps.offendersearch.dto.OffenderDetail
import uk.gov.justice.hmpps.offendersearch.dto.OffenderUserAccess
import uk.gov.justice.hmpps.offendersearch.dto.SearchDto
import uk.gov.justice.hmpps.offendersearch.dto.SearchPhraseFilter
import uk.gov.justice.hmpps.offendersearch.dto.SearchPhraseResults
import java.util.*

@Service
class SearchService @Autowired constructor(private val offenderAccessService: OffenderAccessService, private val hlClient: SearchClient, private val mapper: ObjectMapper) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
    private const val MAX_SEARCH_RESULTS = 100
  }

  fun performSearch(searchOptions: SearchDto): List<OffenderDetail> {
    validateSearchForm(searchOptions)
    val searchRequest = SearchRequest("offender")
    val searchSourceBuilder = SearchSourceBuilder()
    // Set the maximum search result size (the default would otherwise be 10)
    searchSourceBuilder.size(MAX_SEARCH_RESULTS)
    val matchingAllFieldsQuery = buildMatchWithAllProvidedParameters(searchOptions)
    searchSourceBuilder.query(matchingAllFieldsQuery.withDefaults())
    searchRequest.source(searchSourceBuilder)
    val response = hlClient.search(searchRequest)
    return getSearchResult(response)
  }

  protected fun buildMatchWithAllProvidedParameters(searchOptions: SearchDto): BoolQueryBuilder {
    val matchingAllFieldsQuery = QueryBuilders
        .boolQuery()
    with(searchOptions) {
      croNumber.takeIf { !it.isNullOrBlank() }?.let {
        matchingAllFieldsQuery
            .mustKeyword(it.toLowerCase(), "otherIds.croNumberLowercase")
      }
      pncNumber.takeIf { !it.isNullOrBlank() }?.let {
        matchingAllFieldsQuery
            .mustMultiMatchKeyword(it.canonicalPNCNumber(), "otherIds.pncNumberLongYear", "otherIds.pncNumberShortYear")
      }
      matchingAllFieldsQuery
          .mustWhenPresent("otherIds.nomsNumber", nomsNumber)
          .mustWhenPresent("otherIds.crn", crn)
          .mustWhenPresent("firstName", firstName)
          .mustWhenPresent("surname", surname)
          .mustWhenPresent("dateOfBirth", dateOfBirth)
    }

    return matchingAllFieldsQuery
  }

  private fun validateSearchForm(searchOptions: SearchDto) {
    if (!searchOptions.isValid) {
      log.warn("Invalid search  - no criteria provided")
      throw BadRequestException("Invalid search  - please provide at least 1 search parameter")
    }
  }

  private fun getSearchResult(response: SearchResponse): List<OffenderDetail> {
    val searchHit = response.hits.hits
    val offenderDetailList = ArrayList<OffenderDetail>()
    if (searchHit.isNotEmpty()) {
      Arrays.stream(searchHit)
          .forEach { hit: SearchHit ->
            offenderDetailList
                .add(parseOffenderDetail(hit.sourceAsString))
          }
    }
    return offenderDetailList
  }

  private fun parseOffenderDetail(src: String): OffenderDetail {
    return try {
      mapper.readValue(src, OffenderDetail::class.java)
    } catch (t: Throwable) {
      throw RuntimeException(t)
    }
  }

  private fun BoolQueryBuilder.withDefaults(): BoolQueryBuilder? {
    return this
        .must("softDeleted", false)
  }

  fun performSearch(searchPhraseFilter: SearchPhraseFilter, pageable: Pageable, offenderUserAccess: OffenderUserAccess): SearchPhraseResults {

    fun canAccessOffender(offenderDetail: OffenderDetail): Boolean {
      return offenderAccessService.canAccessOffender(offenderDetail, offenderUserAccess)
    }

    log.info("Search was: \"${searchPhraseFilter.phrase}\"")
    val searchRequest = SearchRequest("offender")
        .source(SearchSourceBuilder()
            .query(buildQuery(searchPhraseFilter.phrase, searchPhraseFilter.matchAllTerms))
            .size(pageable.pageSize)
            .from(pageable.offset.toInt())
            .sort("_score")
            .sort("offenderId", DESC)
            .trackTotalHits(true)
            .aggregation(buildAggregationRequest())
            .highlighter(buildHighlightRequest())
            .suggest(SuggestBuilder()
                .addSuggestion("surname", TermSuggestionBuilder("surname").text(searchPhraseFilter.phrase))
                .addSuggestion("firstName", TermSuggestionBuilder("firstName").text(searchPhraseFilter.phrase)))
            .apply {
              buildProbationAreaFilter(searchPhraseFilter.probationAreasFilter)?.run { postFilter(this) }
            }
        )
    val response = hlClient.search(searchRequest)
    return SearchPhraseResults(
        content = extractOffenderDetailList(
            hits = response.hits.hits,
            phrase = searchPhraseFilter.phrase,
            offenderParser = ::parseOffenderDetail,
            accessChecker = ::canAccessOffender
        ),
        pageable = pageable,
        total = response.hits.totalHits?.value ?: 0,
        probationAreaAggregations = extractProbationAreaAggregation(response.aggregations),
        suggestions = response.suggest
    )
  }

  fun findByListOfCRNs(crnList: List<String>): List<OffenderDetail> {
    val searchRequest = SearchRequest("offender")
    val searchSourceBuilder = SearchSourceBuilder()

    searchSourceBuilder.size(crnList.size)

    val outerMustQuery = QueryBuilders
        .boolQuery()

    val matchingAllFieldsQuery = QueryBuilders
        .boolQuery()
    crnList.forEach {
      matchingAllFieldsQuery
          .should(QueryBuilders.matchQuery("otherIds.crn", it))
    }
    outerMustQuery.must(matchingAllFieldsQuery)
    searchSourceBuilder.query(outerMustQuery.withDefaults())
    searchRequest.source(searchSourceBuilder)
    val response = hlClient.search(searchRequest)
    return getSearchResult(response)
  }
}