package uk.gov.justice.hmpps.offendersearch.dto

import io.swagger.v3.oas.annotations.media.Schema

data class StaffHuman(
  @Schema(description = "Staff code", example = "AN001A") val code: String? = null,
  @Schema(description = "Given names", example = "Sheila Linda") val forenames: String? = null,
  @Schema(description = "Family name", example = "Hancock") val surname: String? = null,
  @Schema(description = "When true the not allocated", example = "false") val unallocated: Boolean? = null
)
