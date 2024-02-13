package uk.gov.justice.hmpps.probationsearch.config

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import uk.gov.justice.hmpps.probationsearch.security.AuthAwareAuthenticationToken

@Component
class SecurityUserContext {
  val authentication: AuthAwareAuthenticationToken?
    get() = with(SecurityContextHolder.getContext().authentication) {
      when (this) {
        is AuthAwareAuthenticationToken -> this
        else -> null
      }
    }

  val currentUsername: String?
    get() = authentication?.subject
  val currentDeliusUsername: String?
    get() = authentication?.takeIf { it.deliusUser }?.subject
  val token: String?
    get() = authentication?.token?.tokenValue
}
