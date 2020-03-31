package uk.gov.justice.hmpps.offendersearch.dto

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import uk.gov.justice.hmpps.offendersearch.dto.MatchedBy.NOTHING


data class OffenderMatches(
    @ApiModelProperty(required = true, value = "List of offenders that share the same possibility of being the match")
    val matches: List<OffenderMatch> = listOf(),
    @ApiModelProperty(required = true, value = "How the match was performed")
    val matchedBy: MatchedBy = NOTHING
)

@ApiModel
enum class MatchedBy {
  ALL_SUPPLIED,
  HMPPS_KEY,
  EXTERNAL_KEY,
  NOTHING
}