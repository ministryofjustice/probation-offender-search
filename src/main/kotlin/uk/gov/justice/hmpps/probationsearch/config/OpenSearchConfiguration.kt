package uk.gov.justice.hmpps.probationsearch.config

import org.apache.http.HttpHost
import org.opensearch.client.RestClient
import org.opensearch.client.RestHighLevelClient
import org.opensearch.data.client.orhlc.OpenSearchRestTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenSearchConfiguration {
  @Value("\${opensearch.uris}")
  private val url: String? = null

  @Bean
  fun openSearchClient() = RestHighLevelClient(RestClient.builder(HttpHost.create(url)))

  @Bean(name = ["elasticsearchTemplate", "elasticsearchOperations"])
  @ConditionalOnMissingBean(OpenSearchRestTemplate::class)
  fun openSearchRestTemplate(client: RestHighLevelClient) = OpenSearchRestTemplate(client)
}
