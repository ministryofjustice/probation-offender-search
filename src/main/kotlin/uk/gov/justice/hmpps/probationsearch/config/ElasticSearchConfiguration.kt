package uk.gov.justice.hmpps.probationsearch.config

import com.amazonaws.auth.AWS4Signer
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import org.apache.http.HttpHost
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate

@Configuration
class ElasticSearchConfiguration {
  @Value("\${elasticsearch.port}")
  private val port = 0

  @Value("\${elasticsearch.host}")
  private val host: String? = null

  @Value("\${elasticsearch.scheme}")
  private val scheme: String? = null

  @Value("\${elasticsearch.aws.signrequests:false}")
  private val shouldSignRequests = false

  @Value("\${aws.region:eu-west-2}")
  private val awsRegion: String? = null

  @Bean
  fun elasticSearchClient(): RestHighLevelClient {
    if (shouldSignRequests) {
      val signer = AWS4Signer()
      signer.serviceName = SERVICE_NAME
      signer.regionName = awsRegion
      val clientBuilder = RestClient.builder(HttpHost(host, port, scheme)).setHttpClientConfigCallback { callback: HttpAsyncClientBuilder ->
        callback.addInterceptorLast(
          AWSRequestSigningApacheInterceptor(SERVICE_NAME, signer, DefaultAWSCredentialsProviderChain()),
        )
      }
      return RestHighLevelClient(clientBuilder)
    }
    return RestHighLevelClient(RestClient.builder(HttpHost(host, port, scheme)))
  }

  @Bean(name = ["elasticsearchTemplate", "elasticsearchOperations"])
  @ConditionalOnMissingBean(ElasticsearchRestTemplate::class)
  fun elasticsearchTemplate(client: RestHighLevelClient) = ElasticsearchRestTemplate(client)

  companion object {
    const val SERVICE_NAME = "es"
  }
}
