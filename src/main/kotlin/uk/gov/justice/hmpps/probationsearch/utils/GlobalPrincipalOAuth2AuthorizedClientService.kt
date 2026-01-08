package uk.gov.justice.hmpps.probationsearch.utils

import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientId
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * OAuth2AuthorizedClientService implementation that caches clients using the single global principal name. Clients are
 * cached against a [org.springframework.security.oauth2.client.OAuth2AuthorizedClientId] key constructed from:
 * - **clientRegistrationId** And
 * - **principalName** - Hardcoded to `global-system-principal`.
 *
 * The default implementation [org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService] sets
 * the **principalName** to the **principal** extracted from the current [org.springframework.security.core.Authentication] object.
 * HMPPS digital service use the OAuth 2.0 client credentials grant for service-to-service calls, however HMPPS Auth provides the ability to embed
 * the username of user initiating the action in the system client token for additional context. When using the
 * [uk.gov.justice.hmpps.kotlin.auth.AuthAwareAuthenticationToken], if there is a username claim present in the token then
 * this will be used as the authenticated principal instead of the system client id. This means that without the global principal
 * a new token will be created per user session. During periods of heavy traffic this creates additional load on the Auth token endpoint.
 */
@Component
class GlobalPrincipalOAuth2AuthorizedClientService(
  private val clientRegistrationRepository: ClientRegistrationRepository,
) : OAuth2AuthorizedClientService {

  companion object {
    const val GLOBAL_SYSTEM_PRINCIPAL = "global-system-principal"
  }

  private val authorizedClients: MutableMap<OAuth2AuthorizedClientId, OAuth2AuthorizedClient> =
    ConcurrentHashMap()

  override fun <T : OAuth2AuthorizedClient?> loadAuthorizedClient(
    clientRegistrationId: String,
    principalName: String,
  ): T? = clientRegistrationRepository.findByRegistrationId(clientRegistrationId)?.let {
    @Suppress("UNCHECKED_CAST")
    authorizedClients[OAuth2AuthorizedClientId(clientRegistrationId, GLOBAL_SYSTEM_PRINCIPAL)] as T
  }

  override fun saveAuthorizedClient(authorizedClient: OAuth2AuthorizedClient, principal: Authentication) {
    authorizedClients[
      OAuth2AuthorizedClientId(authorizedClient.clientRegistration.registrationId, GLOBAL_SYSTEM_PRINCIPAL),
    ] = authorizedClient
  }

  override fun removeAuthorizedClient(clientRegistrationId: String, principalName: String) {
    clientRegistrationRepository.findByRegistrationId(clientRegistrationId)?.apply {
      authorizedClients.remove(OAuth2AuthorizedClientId(clientRegistrationId, GLOBAL_SYSTEM_PRINCIPAL))
    }
  }
}