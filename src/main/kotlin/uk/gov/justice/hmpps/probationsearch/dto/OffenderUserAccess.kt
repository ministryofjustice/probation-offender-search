package uk.gov.justice.hmpps.probationsearch.dto

/*
  A user may or may not be present in the security context.
  When the user is present in the security context the following apply:
  1) Access to offenders that have an exclusion list will be allowed when either:
      a) this user is not on the exclusion list
      b) client has ignoreExclusionsAlways scope
  2) Access to offenders that have an inclusion (restriction) will be allowed when either
      a) this user is on the inclusion (restricted) list
      b) client has ignoreInclusionsAlways scope

  When a user is not present in the security context the following apply:
  1) Access to offenders that have an exclusion list will be allowed when:
      a) client has ignoreExclusionsAlways scope
  2) Access to offenders that have an inclusion (restriction) will be allowed when
      a) client has ignoreInclusionsAlways scope
 */
data class OffenderUserAccess(
  val username: String? = null,
  val ignoreExclusionsAlways: Boolean = false,
  val ignoreInclusionsAlways: Boolean = false,
)
