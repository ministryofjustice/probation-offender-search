package uk.gov.justice.hmpps.probationsearch.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.codec.json.JacksonJsonEncoder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.hmpps.kotlin.auth.HmppsAuthenticationHolder
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient

@Configuration
class WebClientConfiguration(
  @param:Value($$"${community.endpoint.url}") private val communityRootUri: String,
  @param:Value($$"${delius.endpoint.url}") private val deliusRootUri: String,
  private val jsonMapper: JsonMapper,
  private val authenticationHolder: HmppsAuthenticationHolder,
) {

  @Bean
  fun communityApiWebClient(): WebClient {
    return WebClient.builder()
      .baseUrl(communityRootUri)
      .filter(addAuthHeaderFilterFunction())
      .build()
  }

  @Bean
  fun searchAndDeliusApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient =
    WebClient.builder()
      .codecs { it.defaultCodecs().jacksonJsonEncoder(JacksonJsonEncoder(jsonMapper)) }
      .authorisedWebClient(authorizedClientManager, "probation-search-and-delius", deliusRootUri)

  private fun addAuthHeaderFilterFunction(): ExchangeFilterFunction {
    return ExchangeFilterFunction { request: ClientRequest, next: ExchangeFunction ->
      val filtered = ClientRequest.from(request)
        .header(HttpHeaders.AUTHORIZATION, "Bearer ${authenticationHolder.authentication.jwt.tokenValue}")
        .build()
      next.exchange(filtered)
    }
  }
}
