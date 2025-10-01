package uk.gov.justice.hmpps.probationsearch.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.pool.PoolConcurrencyPolicy
import org.apache.hc.core5.util.Timeout
import org.opensearch.client.RestClient
import org.opensearch.client.RestHighLevelClient
import org.opensearch.client.json.jackson.JacksonJsonpMapper
import org.opensearch.data.client.orhlc.OpenSearchRestTemplate
import org.opensearch.spring.boot.autoconfigure.RestClientBuilderCustomizer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class OpenSearchConfiguration {
  @Value($$"${opensearch.uris}")
  private val url: String? = null

  @Bean
  fun openSearchClient() = RestHighLevelClient(RestClient.builder(HttpHost.create(url)))

  @Bean
  fun openSearchRestClientCustomizer(): RestClientBuilderCustomizer = RestClientBuilderCustomizer { builder ->
    builder
      .setHttpClientConfigCallback {
        it.setConnectionManager(
          PoolingAsyncClientConnectionManagerBuilder.create()
            .setPoolConcurrencyPolicy(PoolConcurrencyPolicy.LAX)
            .setMaxConnPerRoute(50) // Increase pool size to handle parallel on-demand data load (see ContactDataLoadService.kt)
            .build(),
        )
      }
      .setRequestConfigCallback { it.setConnectionRequestTimeout(Timeout.ofSeconds(30)) }
  }

  @Bean(name = ["elasticsearchTemplate", "elasticsearchOperations"])
  @ConditionalOnMissingBean(OpenSearchRestTemplate::class)
  fun openSearchRestTemplate(client: RestHighLevelClient) = OpenSearchRestTemplate(client)

  @Bean
  @Primary
  fun jsonpMapper(objectMapper: ObjectMapper) = JacksonJsonpMapper(objectMapper)
}
