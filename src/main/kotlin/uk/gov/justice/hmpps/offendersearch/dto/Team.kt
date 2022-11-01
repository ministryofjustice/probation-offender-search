package uk.gov.justice.hmpps.offendersearch.dto

import io.swagger.v3.oas.annotations.media.Schema

data class Team(
  @Schema(description = "Team code", example = "C01T04") val code: String? = null,
  @Schema(description = "Team description", example = "OMU A") val description: String? = null,
  @Schema(description = "Team telephone, often not populated", required = false, example = "OMU A") val telephone: String? = null,
  @Schema(description = "Local Delivery Unit - LDU") val localDeliveryUnit: KeyValue? = null,
  @Schema(description = "Team's district") val district: KeyValue? = null,
  @Schema(description = "Team's borough") val borough: KeyValue? = null
)
