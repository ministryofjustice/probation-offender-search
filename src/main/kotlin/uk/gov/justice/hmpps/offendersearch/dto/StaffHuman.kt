package uk.gov.justice.hmpps.offendersearch.dto

import io.swagger.annotations.ApiModelProperty

data class StaffHuman(
  @ApiModelProperty(value = "Staff code", example = "AN001A") val code: String? = null,
  @ApiModelProperty(value = "Given names", example = "Sheila Linda") val forenames: String? = null,
  @ApiModelProperty(value = "Family name", example = "Hancock") val surname: String? = null,
  @ApiModelProperty(value = "When true the not allocated", example = "false") val unallocated: Boolean? = null
)
