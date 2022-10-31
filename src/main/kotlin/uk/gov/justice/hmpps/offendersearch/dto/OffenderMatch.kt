package uk.gov.justice.hmpps.offendersearch.dto

import io.swagger.v3.oas.annotations.media.Schema

data class OffenderMatch(
  @Schema(required = true, description = "Details of the matching offender")
  val offender: OffenderDetail
)
