package uk.gov.justice.hmpps.probationsearch.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.codec.json.JacksonJsonEncoder
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.hmpps.probationsearch.utils.GlobalPrincipalOAuth2AuthorizedClientService

@Configuration
class WebClientConfiguration(
  @param:Value($$"${community.endpoint.url}") private val communityRootUri: String,
  @param:Value($$"${delius.endpoint.url}") private val deliusRootUri: String,
  private val jsonMapper: JsonMapper,
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
  fun searchAndDeliusApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    oauth2Client.setDefaultClientRegistrationId("probation-search-and-delius")
    return WebClient.builder()
      .baseUrl(deliusRootUri)
      .apply(oauth2Client.oauth2Configuration())
      .codecs { it.defaultCodecs().jacksonJsonEncoder(JacksonJsonEncoder(jsonMapper)) }
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
  fun authorizedClientManager(
    clientRegistrationRepository: ClientRegistrationRepository?,
    globalPrincipalOAuth2AuthorizedClientService: GlobalPrincipalOAuth2AuthorizedClientService?,
  ): OAuth2AuthorizedClientManager? {
    val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build()
    val authorizedClientManager =
      AuthorizedClientServiceOAuth2AuthorizedClientManager(
        clientRegistrationRepository,
        globalPrincipalOAuth2AuthorizedClientService
      )
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)
    return authorizedClientManager
  }
}
