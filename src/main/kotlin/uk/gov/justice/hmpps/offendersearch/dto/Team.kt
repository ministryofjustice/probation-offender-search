package uk.gov.justice.hmpps.offendersearch.dto

import io.swagger.annotations.ApiModelProperty

data class Team(
    @ApiModelProperty(value = "Team code", example = "C01T04") val code: String? = null,
    @ApiModelProperty(value = "Team description", example = "OMU A") val description: String? = null,
    @ApiModelProperty(value = "Team telephone, often not populated", required = false, example = "OMU A") val telephone: String? = null,
    @ApiModelProperty(value = "Local Delivery Unit - LDU") val localDeliveryUnit: KeyValue? = null,
    @ApiModelProperty(value = "Team's district") val district: KeyValue? = null,
    @ApiModelProperty(value = "Team's borough") val borough: KeyValue? = null
) 