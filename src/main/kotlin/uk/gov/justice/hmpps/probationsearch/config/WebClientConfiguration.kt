package uk.gov.justice.hmpps.probationsearch.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfiguration(
  @Value("\${community.endpoint.url}") private val communityRootUri: String,
  @Value("\${delius.endpoint.url}") private val deliusRootUri: String,
  private val securityUserContext: SecurityUserContext,
) {

  @Bean
  fun communityApiWebClient(): WebClient {
    return WebClient.builder()
      .baseUrl(communityRootUri)
      .filter(addAuthHeaderFilterFunction())
      .build()
  }

  @Bean
  fun searchAndDeliusApiWebClient(): WebClient {
    return WebClient.builder()
      .baseUrl(deliusRootUri)
      .filter(addAuthHeaderFilterFunction())
      .build()
  }

  private fun addAuthHeaderFilterFunction(): ExchangeFilterFunction {
    return ExchangeFilterFunction { request: ClientRequest, next: ExchangeFunction ->
      val filtered = ClientRequest.from(request)
        .header(HttpHeaders.AUTHORIZATION, "Bearer ${securityUserContext.token}")
        .build()
      next.exchange(filtered)
    }
  }

  @Bean
  fun communityApiHealthWebClient(): WebClient {
    return WebClient.builder().baseUrl(communityRootUri).build()
  }
}
