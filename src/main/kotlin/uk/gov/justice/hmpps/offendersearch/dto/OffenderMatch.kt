package uk.gov.justice.hmpps.offendersearch.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.annotations.ApiModelProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OffenderMatch(
  @ApiModelProperty(required = true, value = "Details of the matching offender")
  val offender: OffenderDetail,
  @ApiModelProperty(required = false, value = "Match probability score if available")
  val matchProbability: Double? = null
)
