package uk.gov.justice.hmpps.offendersearch.dto

import io.swagger.v3.oas.annotations.media.Schema

data class IDs(
  @Schema(required = true) val crn: String? = null,
  val pncNumber: String? = null,
  val croNumber: String? = null,
  val niNumber: String? = null,
  val nomsNumber: String? = null,
  val immigrationNumber: String? = null,
  val mostRecentPrisonerNumber: String? = null
)
