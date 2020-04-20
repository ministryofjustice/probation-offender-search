package uk.gov.justice.hmpps.offendersearch.config

import com.amazonaws.auth.AWS4Signer
import com.amazonaws.auth.BasicSessionCredentials
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider
import com.amazonaws.auth.STSSessionCredentialsProvider
import com.amazonaws.services.securitytoken.AWSSecurityTokenService
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest
import com.amazonaws.services.securitytoken.model.Credentials
import org.apache.http.HttpHost
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class ElasticSearchConfiguration {
  @Value("\${elasticsearch.port}")
  private val port = 0
  @Value("\${elasticsearch.host}")
  private val host: String? = null
  @Value("\${elasticsearch.scheme}")
  private val scheme: String? = null
  @Value("\${elasticsearch.aws.signrequests}")
  private val shouldSignRequests = false
  @Value("\${aws.region:eu-west-2}")
  private val awsRegion: String? = null
  @Value("\${aws.roleArn:test-role-arn}")
  private val roleARN: String? = null
  @Value("\${aws.roleSessionName:test-role-session}")
  private val roleSessionName: String? = null

  @Bean
  fun elasticSearchClient(): RestHighLevelClient {
    if (shouldSignRequests) {

      val stsClient: AWSSecurityTokenService = AWSSecurityTokenServiceClientBuilder.standard()
              .withCredentials(STSAssumeRoleSessionCredentialsProvider(roleARN, roleSessionName))
              .withRegion(awsRegion)
              .build()

      val roleRequest = AssumeRoleRequest()
              .withRoleArn(roleARN)
              .withRoleSessionName(roleSessionName)
      val roleResponse = stsClient.assumeRole(roleRequest)
      val sessionCredentials: Credentials = roleResponse.credentials

      val awsCredentials = BasicSessionCredentials(
              sessionCredentials.accessKeyId,
              sessionCredentials.secretAccessKey,
              sessionCredentials.sessionToken)

      val signer = AWS4Signer()
      signer.serviceName = SERVICE_NAME
      signer.regionName = awsRegion
      val clientBuilder = RestClient.builder(HttpHost(host, port, scheme)).setHttpClientConfigCallback { callback: HttpAsyncClientBuilder ->
        callback.addInterceptorLast(
            AWSRequestSigningApacheInterceptor(SERVICE_NAME, signer, STSSessionCredentialsProvider(awsCredentials)))
      }
      return RestHighLevelClient(clientBuilder)
    }
    return RestHighLevelClient(RestClient.builder(HttpHost(host, port, scheme)))
  }

  companion object {
    const val SERVICE_NAME = "es"
  }
}