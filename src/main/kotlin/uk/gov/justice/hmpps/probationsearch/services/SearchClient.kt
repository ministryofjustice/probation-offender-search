package uk.gov.justice.hmpps.probationsearch.services

import org.opensearch.action.search.SearchRequest
import org.opensearch.action.search.SearchResponse
import org.opensearch.client.RequestOptions
import org.opensearch.client.RestHighLevelClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class SearchClient(
  @param:Qualifier("openSearchClient") private val openSearchClient: RestHighLevelClient,
) {
  fun search(searchRequest: SearchRequest): SearchResponse = openSearchClient.search(searchRequest, RequestOptions.DEFAULT)
}
