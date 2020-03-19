package uk.gov.justice.hmpps.offendersearch.dto

import io.swagger.annotations.ApiModelProperty
import org.apache.commons.lang3.StringUtils
import java.time.LocalDate

data class SearchDto(//todo confirm description and examples
    @ApiModelProperty(required = false, value = "Offender first name(s)", position = 1) val firstName: String? = null,
    @ApiModelProperty(required = false, value = "Offender surname", position = 2) val surname: String? = null,
    @ApiModelProperty(required = false, value = "Offender date of birth", example = "1996-02-10", position = 3) val dateOfBirth: LocalDate? = null,
    @ApiModelProperty(required = false, value = "Police National Computer (PNC) number", example = "2018/0123456X", position = 4) val pncNumber: String? = null,
    @ApiModelProperty(required = false, value = "Criminal Records Office (CRO) number", example = "SF80/655108T", position = 5) val croNumber: String? = null,
    @ApiModelProperty(required = false, value = "Case reference number", example = "X00001", position = 6) val crn: String? = null,
    @ApiModelProperty(required = false, value = "The Offender NOMIS Id (aka prison number/offender no in DPS)", example = "G5555TT", position = 7) val nomsNumber: String? = null
) {

  val isValid: Boolean
    get() = StringUtils.isNotBlank(firstName) || StringUtils.isNotBlank(surname) || dateOfBirth != null || StringUtils.isNotBlank(pncNumber) || StringUtils.isNotBlank(crn) ||
        StringUtils.isNotBlank(nomsNumber) || StringUtils.isNotBlank(croNumber)
}