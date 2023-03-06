package uk.gov.justice.hmpps.probationsearch.security

import org.springframework.security.core.Authentication
import uk.gov.justice.hmpps.probationsearch.config.SecurityUserContext
import uk.gov.justice.hmpps.probationsearch.dto.OffenderUserAccess

fun getOffenderUserAccessFromScopes(securityUserContext: SecurityUserContext): OffenderUserAccess = OffenderUserAccess(
  username = securityUserContext.currentDeliusUsername,
  ignoreExclusionsAlways = securityUserContext.authentication.hasScope("ignore_delius_exclusions_always"),
  ignoreInclusionsAlways = securityUserContext.authentication.hasScope("ignore_delius_inclusions_always")
)

private fun Authentication?.hasScope(scope: String): Boolean = this?.authorities?.any { it.authority == "SCOPE_$scope" }
  ?: false
