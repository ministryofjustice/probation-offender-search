package uk.gov.justice.hmpps.offendersearch.dto

import io.swagger.v3.oas.annotations.media.Schema

data class Institution(
  @Schema(required = true) val institutionId: Long? = null,
  val isEstablishment: Boolean? = null,
  val code: String? = null,
  val description: String? = null,
  val institutionName: String? = null,
  val establishmentType: KeyValue? = null,
  val isPrivate: Boolean? = null,
  @Schema(description = "Prison institution code in NOMIS") val nomsPrisonInstitutionCode: String? = null
)
