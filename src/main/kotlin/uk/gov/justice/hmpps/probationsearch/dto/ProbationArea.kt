package uk.gov.justice.hmpps.probationsearch.dto

import io.swagger.v3.oas.annotations.media.Schema

data class ProbationArea(
  @Schema(required = true) val probationAreaId: Long? = null,
  val code: String? = null,
  val description: String? = null,
  val nps: Boolean? = null,
  val organisation: KeyValue? = null,
  val institution: Institution? = null,
  val teams: List<AllTeam>? = null,
)
