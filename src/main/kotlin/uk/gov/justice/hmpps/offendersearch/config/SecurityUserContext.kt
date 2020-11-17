package uk.gov.justice.hmpps.offendersearch.config

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import uk.gov.justice.hmpps.offendersearch.security.AuthAwareAuthenticationToken

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
    get() = authentication?.takeUnless { it.clientOnly }?.subject
  val currentDeliusUsername: String?
    get() = authentication?.takeUnless { it.clientOnly }?.takeIf { it.deliusUser }?.subject
  val token: String?
    get() = authentication?.token?.tokenValue
}
