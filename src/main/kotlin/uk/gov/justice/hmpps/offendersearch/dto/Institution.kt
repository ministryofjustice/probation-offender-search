package uk.gov.justice.hmpps.offendersearch.dto

import io.swagger.annotations.ApiModelProperty

data class Institution(
    @ApiModelProperty(required = true) val institutionId: Long? = null,
    val isEstablishment: Boolean? = null,
    val code: String? = null,
    val description: String? = null,
    val institutionName: String? = null,
    val establishmentType: KeyValue? = null,
    val isPrivate: Boolean? = null,
    @ApiModelProperty(value = "Prison institution code in NOMIS") val nomsPrisonInstitutionCode: String? = null
)