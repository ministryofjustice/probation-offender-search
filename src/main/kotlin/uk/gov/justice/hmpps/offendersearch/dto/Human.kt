package uk.gov.justice.hmpps.offendersearch.dto

import io.swagger.annotations.ApiModelProperty

data class Human(
    @ApiModelProperty(value = "Given names", example = "Sheila Linda") val forenames: String? = null,
    @ApiModelProperty(value = "Family name", example = "Hancock") val surname: String? = null
)