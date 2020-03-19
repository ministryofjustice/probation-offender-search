package uk.gov.justice.hmpps.offendersearch.dto

import io.swagger.annotations.ApiModelProperty


data class OffenderMatches(
    @ApiModelProperty(required = true, value = "List of offenders that share the same possibility of being the match")
    val matches: List<OffenderMatch>
)