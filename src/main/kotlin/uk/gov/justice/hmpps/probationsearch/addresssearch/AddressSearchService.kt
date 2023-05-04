package uk.gov.justice.hmpps.probationsearch.addresssearch

import com.fasterxml.jackson.databind.ObjectMapper
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.search.SearchHits
import org.springframework.stereotype.Service

@Service
class AddressSearchService(val elasticSearchClient: RestHighLevelClient, val objectMapper: ObjectMapper) {
  fun performSearch(addressSearchRequest: AddressSearchRequest, maxResults: Int): AddressSearchResponses {
    val searchSourceBuilder = matchAddresses(addressSearchRequest, maxResults)
    val searchRequest = SearchRequest("person-search-primary")
    searchRequest.source(searchSourceBuilder)
    val res = elasticSearchClient.search(searchRequest, RequestOptions.DEFAULT)
    return AddressSearchResponses(res.hits.toAddressSearchResponse(maxResults))
  }

  fun SearchHits.toAddressSearchResponse(maxResults: Int): List<AddressSearchResponse> = hits
    .flatMap { hit ->
      val personDetail = objectMapper.readValue(hit.sourceAsString, PersonDetail::class.java).toPerson()
      hit.innerHits.values
        .flatMap { it.hits.toList() }
        .filter { it.matchedQueries.matches() }
        .map {
          AddressSearchResponse(
            personDetail,
            objectMapper.readValue(it.sourceAsString, PersonAddress::class.java).toAddress(),
            it.score,
          )
        }
    }
    .sortedWith(compareByDescending<AddressSearchResponse> { it.matchScore }.thenBy { it.address.id })
    .take(maxResults)

  private fun Array<String>.matches(): Boolean = map {
    when (it) {
      "postcode" -> 2
      "streetName", "town" -> 1
      else -> 0
    }
  }.sum() >= 2
}
