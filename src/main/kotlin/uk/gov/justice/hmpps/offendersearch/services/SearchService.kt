package uk.gov.justice.hmpps.offendersearch.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.lang3.StringUtils
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.offendersearch.BadRequestException
import uk.gov.justice.hmpps.offendersearch.dto.OffenderDetail
import uk.gov.justice.hmpps.offendersearch.dto.SearchDto
import java.util.*

@Service
class SearchService @Autowired constructor(private val hlClient: SearchClient, private val mapper: ObjectMapper) {
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
    searchSourceBuilder.query(matchingAllFieldsQuery)
    searchRequest.source(searchSourceBuilder)
    val response = hlClient.search(searchRequest)
    return getSearchResult(response)
  }

  protected fun buildMatchWithAllProvidedParameters(searchOptions: SearchDto): BoolQueryBuilder {
    val matchingAllFieldsQuery = QueryBuilders
        .boolQuery()
    if (StringUtils.isNotBlank(searchOptions.surname)) {
      matchingAllFieldsQuery.must(QueryBuilders
          .matchQuery("surname", searchOptions.surname))
    }
    if (StringUtils.isNotBlank(searchOptions.firstName)) {
      matchingAllFieldsQuery.must(QueryBuilders
          .matchQuery("firstName", searchOptions.firstName))
    }
    if (searchOptions.dateOfBirth != null) {
      matchingAllFieldsQuery.must(QueryBuilders
          .matchQuery("dateOfBirth", searchOptions.dateOfBirth))
    }
    if (StringUtils.isNotBlank(searchOptions.crn)) {
      matchingAllFieldsQuery.must(QueryBuilders
          .matchQuery("otherIds.crn", searchOptions.crn))
    }
    if (StringUtils.isNotBlank(searchOptions.croNumber)) {
      matchingAllFieldsQuery.must(QueryBuilders
          .matchQuery("otherIds.croNumber", searchOptions.croNumber))
    }
    if (StringUtils.isNotBlank(searchOptions.pncNumber)) {
      matchingAllFieldsQuery.must(QueryBuilders
          .matchQuery("otherIds.pncNumber", searchOptions.pncNumber))
    }
    if (StringUtils.isNotBlank(searchOptions.nomsNumber)) {
      matchingAllFieldsQuery.must(QueryBuilders
          .matchQuery("otherIds.nomsNumber", searchOptions.nomsNumber))
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

}