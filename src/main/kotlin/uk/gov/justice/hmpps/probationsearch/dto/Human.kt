package uk.gov.justice.hmpps.probationsearch.dto

import io.swagger.v3.oas.annotations.media.Schema

data class Human(
  @Schema(example = "Sheila Linda") val forenames: String? = null,
  @Schema(example = "Hancock") val surname: String? = null,
)
