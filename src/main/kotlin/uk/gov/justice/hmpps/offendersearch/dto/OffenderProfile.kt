package uk.gov.justice.hmpps.offendersearch.dto

data class OffenderProfile(
  val ethnicity: String? = null,
  val nationality: String? = null,
  val secondaryNationality: String? = null,
  val notes: String? = null,
  val immigrationStatus: String? = null,
  val offenderLanguages: OffenderLanguages? = null,
  val religion: String? = null,
  val sexualOrientation: String? = null,
  val remandStatus: String? = null,
  val previousConviction: PreviousConviction? = null,
  val riskColour: String? = null,
  val disabilities: List<Disability>? = null
)
