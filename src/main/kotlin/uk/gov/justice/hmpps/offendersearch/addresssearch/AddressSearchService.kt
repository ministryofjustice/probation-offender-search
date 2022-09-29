package uk.gov.justice.hmpps.offendersearch.addresssearch

import com.fasterxml.jackson.databind.ObjectMapper
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.search.SearchHits
import org.springframework.stereotype.Service

@Service
class AddressSearchService(val elasticSearchClient: RestHighLevelClient, val objectMapper: ObjectMapper) {
  fun performSearch(addressSearchRequest: AddressSearchRequest): List<AddressSearchResponse?> {
    val searchSourceBuilder = matchAddresses(addressSearchRequest)
    val searchRequest = SearchRequest("person-search-standby")
    searchRequest.source(searchSourceBuilder)
    val res = elasticSearchClient.search(searchRequest, RequestOptions.DEFAULT)
    return res.hits.toAddressSearchResponse()
  }

  fun SearchHits.toAddressSearchResponse(): List<AddressSearchResponse> = hits.map { hit ->
    val personDetail = objectMapper.readValue(hit.sourceAsString, PersonDetail::class.java).toPerson()
    val addresses = hit.innerHits.values.flatMap { innerHit ->
      innerHit.hits.map { objectMapper.readValue(it.sourceAsString, PersonAddress::class.java).toAddress(it.score.toDouble()) }
    }
    AddressSearchResponse(personDetail, addresses)
  }
}


