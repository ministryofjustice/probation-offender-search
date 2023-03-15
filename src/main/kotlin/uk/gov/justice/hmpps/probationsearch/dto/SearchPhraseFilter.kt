package uk.gov.justice.hmpps.probationsearch.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class SearchPhraseFilter(
  @field:NotBlank(message = "phrase must be supplied")
  @Schema(required = true, description = "Phrase containing the terms to search for", example = "john smith 19/07/1965")
  val phrase: String = "",
  @Schema(required = false, description = "When true, only match offenders that match all terms. Analogous to AND versus OR") val matchAllTerms: Boolean = false,
  @Schema(required = false, description = "Filter of probation area codes. Only offenders that have an active offender manager in one of the areas will be returned", example = "[\"N01\",\"N02\"]") val probationAreasFilter: List<String> = listOf(),
)
