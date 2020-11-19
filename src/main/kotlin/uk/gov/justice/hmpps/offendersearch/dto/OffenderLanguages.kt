package uk.gov.justice.hmpps.offendersearch.dto

data class OffenderLanguages(
  val primaryLanguage: String? = null,
  val otherLanguages: List<String>? = null,
  val languageConcerns: String? = null,
  val requiresInterpreter: Boolean? = null
)
