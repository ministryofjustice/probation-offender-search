package uk.gov.justice.hmpps.offendersearch.dto

import io.swagger.annotations.ApiModelProperty

data class IDs(
  @ApiModelProperty(required = true) val crn: String? = null,
  val pncNumber: String? = null,
  val croNumber: String? = null,
  val niNumber: String? = null,
  val nomsNumber: String? = null,
  val immigrationNumber: String? = null,
  val mostRecentPrisonerNumber: String? = null
)
