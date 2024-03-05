package uk.gov.justice.hmpps.probationsearch.dto

import io.swagger.v3.oas.annotations.media.Schema

data class IDs(
  @Schema(example = "A123456", description = "Probation Case Reference Number")
  val crn: String,
  @Schema(example = "2012/0052494Q", description = "Police National Computer ID")
  val pncNumber: String? = null,
  @Schema(example = "123456/24A", description = "Criminal Records Office Number")
  val croNumber: String? = null,
  @Schema(example = "AA123456A", description = "National Insurance Number")
  val niNumber: String? = null,
  @Schema(example = "G5555TT", description = "Prison Offender Number")
  val nomsNumber: String? = null,
  val immigrationNumber: String? = null,
  val mostRecentPrisonerNumber: String? = null,
  val previousCrn: String? = null,
)
