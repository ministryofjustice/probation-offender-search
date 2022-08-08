package uk.gov.justice.hmpps.offendersearch.dto

import io.swagger.annotations.ApiModelProperty

data class OffenderMatch(
  @ApiModelProperty(required = true, value = "Details of the matching offender")
  val offender: OffenderDetail,
  @ApiModelProperty(required = false, value = "Match probability score if available")
  val matchProbability: Double? = null
)
