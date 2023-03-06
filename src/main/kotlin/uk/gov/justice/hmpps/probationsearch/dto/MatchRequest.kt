package uk.gov.justice.hmpps.probationsearch.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Past
import java.time.LocalDate

data class MatchRequest(
  @Schema(description = "Offender first description", example = "john") val firstName: String? = null,
  @field:NotBlank(message = "Surname is required")
  @Schema(description = "Offender surname", example = "smith") val surname: String?,
  @field:Past(message = "Date of birth must be in the past")
  @Schema(description = "Offender date of birth", example = "1996-02-10") val dateOfBirth: LocalDate? = null,
  @Schema(description = "Police National Computer (PNC) number", example = "2018/0123456X") val pncNumber: String? = null,
  @Schema(description = "Criminal Records Office (CRO) number", example = "SF80/655108T") val croNumber: String? = null,
  @Schema(description = "The Offender NOMIS Id (aka prison number/offender no in DPS)", example = "G5555TT") val nomsNumber: String? = null,
  @Schema(description = "Filter so only offenders on a current sentence managed by probation will be returned", example = "true") val activeSentence: Boolean = false
)
