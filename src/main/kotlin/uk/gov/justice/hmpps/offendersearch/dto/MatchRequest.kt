package uk.gov.justice.hmpps.offendersearch.dto

import io.swagger.annotations.ApiModelProperty
import java.time.LocalDate
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Past

data class MatchRequest(
  @ApiModelProperty(value = "Offender first name", example = "john", position = 1) val firstName: String? = null,
  @field:NotBlank(message = "Surname is required")
  @ApiModelProperty(value = "Offender surname", example = "smith", position = 2) val surname: String?,
  @field:Past(message = "Date of birth must be in the past")
  @ApiModelProperty(value = "Offender date of birth", example = "1996-02-10", position = 3) val dateOfBirth: LocalDate? = null,
  @ApiModelProperty(value = "Police National Computer (PNC) number", example = "2018/0123456X", position = 4) val pncNumber: String? = null,
  @ApiModelProperty(value = "Criminal Records Office (CRO) number", example = "SF80/655108T", position = 5) val croNumber: String? = null,
  @ApiModelProperty(value = "The Offender NOMIS Id (aka prison number/offender no in DPS)", example = "G5555TT", position = 7) val nomsNumber: String? = null,
  @ApiModelProperty(value = "Filter so only offenders on a current sentence managed by probation will be returned", example = "true", position = 8) val activeSentence: Boolean = false,
  @ApiModelProperty(value = "If available, the name of the system that the data in this match request has originated from", example = "LIBRA", position = 9) val sourceSystem: String? = null
)
