package uk.gov.justice.hmpps.offendersearch.addresssearch

import com.fasterxml.jackson.databind.ObjectMapper
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.search.SearchHits
import org.springframework.stereotype.Service

@Service
class AddressSearchService(val elasticSearchClient: RestHighLevelClient, val objectMapper: ObjectMapper) {
  fun performSearch(addressSearchRequest: AddressSearchRequest, pageSize: Int, offset: Int): AddressSearchResponses {
    val searchSourceBuilder = matchAddresses(addressSearchRequest, pageSize, offset)
    val searchRequest = SearchRequest("person-search-primary")
    searchRequest.source(searchSourceBuilder)
    val res = elasticSearchClient.search(searchRequest, RequestOptions.DEFAULT)
    return AddressSearchResponses(res.hits.toAddressSearchResponse())
  }

  fun SearchHits.toAddressSearchResponse(): List<AddressSearchResponse> = hits.flatMap { hit ->
    val personDetail = objectMapper.readValue(hit.sourceAsString, PersonDetail::class.java).toPerson()
    hit.innerHits.values.flatMap { innerHit ->
      innerHit.hits.mapNotNull {
        if (it.matchedQueries.isNotEmpty()) {
          objectMapper.readValue(it.sourceAsString, PersonAddress::class.java).toAddress(it.score.toDouble() / maxScore)
        } else {
          null
        }
      }
    }.map { AddressSearchResponse(personDetail, it) }
  }
}
