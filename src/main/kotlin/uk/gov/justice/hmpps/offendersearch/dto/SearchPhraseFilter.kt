package uk.gov.justice.hmpps.offendersearch.dto

import io.swagger.annotations.ApiModelProperty
import javax.validation.constraints.NotBlank

data class SearchPhraseFilter(
    @field:NotBlank(message = "phrase must be supplied")
    @ApiModelProperty(required = true, value = "Phrase containing the terms to search for", example = "john smith 19/07/1965") val phrase: String = "",
    @ApiModelProperty(required = false, value = "When true, only match offenders that match all terms. Analogous to AND versus OR") val matchAllTerms: Boolean = false,
    @ApiModelProperty(required = false, value = "Filter of probation area codes. Only offenders that have an active offender manager in one of the areas will be returned", example = "[\"N01\",\"N02\"]") val probationAreasFilter: List<String> = listOf()
)