package uk.gov.justice.hmpps.probationsearch.dto

data class Institution(
  val institutionId: Long? = null,
  val isEstablishment: Boolean? = null,
  val code: String? = null,
  val description: String? = null,
  val institutionName: String? = null,
  val establishmentType: KeyValue? = null,
  val isPrivate: Boolean? = null,
  val nomsPrisonInstitutionCode: String? = null
)
