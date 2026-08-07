package uk.gov.justice.hmpps.probationsearch.security

import org.springframework.security.core.Authentication
import uk.gov.justice.hmpps.kotlin.auth.AuthSource
import uk.gov.justice.hmpps.kotlin.auth.HmppsAuthenticationHolder
import uk.gov.justice.hmpps.probationsearch.dto.OffenderUserAccess

fun getOffenderUserAccessFromScopes(authenticationHolder: HmppsAuthenticationHolder): OffenderUserAccess =
  OffenderUserAccess(
    username = authenticationHolder.username?.takeIf { authenticationHolder.authSource == AuthSource.DELIUS },
    ignoreExclusionsAlways = authenticationHolder.authentication.hasScope("ignore_delius_exclusions_always"),
    ignoreInclusionsAlways = authenticationHolder.authentication.hasScope("ignore_delius_inclusions_always"),
  )

private fun Authentication.hasScope(scope: String): Boolean = authorities.any { it.authority == "SCOPE_$scope" }
