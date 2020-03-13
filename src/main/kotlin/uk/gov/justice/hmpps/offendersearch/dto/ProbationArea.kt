package uk.gov.justice.hmpps.offendersearch.dto

import io.swagger.annotations.ApiModelProperty

data class ProbationArea(
    @ApiModelProperty(required = true) val probationAreaId: Long? = null,
    val code: String? = null,
    val description: String? = null,
    val nps: Boolean? = null,
    val organisation: KeyValue? = null,
    val institution: Institution? = null,
    val teams: List<AllTeam>? = null
) {
}