package uk.gov.justice.hmpps.offendersearch.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClient.Builder

@Configuration
class WebClientConfiguration(@Value("\${community.endpoint.url}") private val communityRootUri: String,
                             @Value("\${oauth.endpoint.url}") private val oauthRootUri: String,
                             private val webClientBuilder: Builder,
                             private val securityUserContext: SecurityUserContext) {

  @Bean
  fun communityApiWebClient(): WebClient {
    return webClientBuilder
        .baseUrl(communityRootUri)
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
    return webClientBuilder.baseUrl(communityRootUri).build()
  }


  @Bean
  fun oauthApiHealthWebClient(): WebClient {
    return webClientBuilder.baseUrl(oauthRootUri).build()
  }

  @Bean
  fun authorizedClientManager(clientRegistrationRepository: ClientRegistrationRepository?,
                              oAuth2AuthorizedClientService: OAuth2AuthorizedClientService?): OAuth2AuthorizedClientManager? {
    val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build()
    val authorizedClientManager = AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, oAuth2AuthorizedClientService)
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)
    return authorizedClientManager
  }
}
