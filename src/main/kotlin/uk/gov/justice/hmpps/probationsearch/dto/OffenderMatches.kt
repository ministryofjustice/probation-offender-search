package uk.gov.justice.hmpps.probationsearch.dto

import io.swagger.v3.oas.annotations.media.Schema

data class OffenderMatches(
  @Schema(required = true, description = "List of offenders that share the same possibility of being the match")
  val matches: List<OffenderMatch> = listOf(),
  @Schema(required = true, description = "How the match was performed")
  val matchedBy: MatchedBy = MatchedBy.NOTHING,
)

@Schema
enum class MatchedBy {
  ALL_SUPPLIED,
  ALL_SUPPLIED_ALIAS,
  HMPPS_KEY,
  EXTERNAL_KEY,
  NAME,
  PARTIAL_NAME,
  PARTIAL_NAME_DOB_LENIENT,
  NOTHING,
}
