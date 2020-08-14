package uk.gov.justice.hmpps.offendersearch.dto

import io.swagger.annotations.ApiModelProperty
import javax.validation.constraints.NotBlank

data class SearchPhraseFilter(
    @field:NotBlank(message = "phrase must be supplied")
    @ApiModelProperty(required = true, value = "Phrase containing the terms to search for", example = "john smith 19/07/1965") val phrase: String? = null,
    @ApiModelProperty(required = false, value = "When true, only match offenders that match all terms. Analogous to AND versus OR") val matchAllTerms: Boolean = false,
    @ApiModelProperty(required = false, value = "Filter of probation area codes. Only offenders that have an active offender manager in one of the areas will be returned", example = "[\"N01\",\"N02\"]") val probationAreasFilter: List<String> = listOf(),
    @ApiModelProperty(required = false, value = "Size of page for offender results", example = "20") val size: Int = 10,
    @ApiModelProperty(required = false, value = "Page number for offender results. First page is page 0", example = "5") val page: Int = 0
)