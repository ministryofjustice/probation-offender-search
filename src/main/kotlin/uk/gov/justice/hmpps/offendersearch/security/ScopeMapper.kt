package uk.gov.justice.hmpps.offendersearch.security

import org.springframework.security.core.Authentication
import uk.gov.justice.hmpps.offendersearch.config.SecurityUserContext
import uk.gov.justice.hmpps.offendersearch.dto.OffenderUserAccess


fun getOffenderUserAccessFromScopes(securityUserContext: SecurityUserContext): OffenderUserAccess = OffenderUserAccess(
    username = securityUserContext.currentDeliusUsername,
    ignoreExclusionsAlways = securityUserContext.authentication.hasScope("ignore_delius_exclusions_always"),
    ignoreInclusionsAlways = securityUserContext.authentication.hasScope("ignore_delius_inclusions_always"),
    allowWhenExclusionMatched = securityUserContext.authentication.hasScope("allow_when_delius_exclusion_matched"),
    allowWhenInclusionNotMatched = securityUserContext.authentication.hasScope("allow_when_delius_inclusion_not_matched")
)

private fun Authentication?.hasScope(scope: String): Boolean = this?.authorities?.any { it.authority == "SCOPE_$scope" }
    ?: false

