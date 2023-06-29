package uk.gov.justice.hmpps.probationsearch.dto

import io.swagger.v3.oas.annotations.media.Schema
import org.apache.commons.lang3.StringUtils
import java.time.LocalDate

data class SearchDto( // todo confirm description and examples
  @Schema(required = false) val firstName: String? = null,
  @Schema(required = false) val surname: String? = null,
  @Schema(required = false, example = "1996-02-10") val dateOfBirth: LocalDate? = null,
  @Schema(required = false, example = "2018/0123456X") val pncNumber: String? = null,
  @Schema(required = false, example = "SF80/655108T") val croNumber: String? = null,
  @Schema(required = false, example = "X00001") val crn: String? = null,
  @Schema(required = false, example = "G5555TT") val nomsNumber: String? = null,
  @Schema(required = false, example = "false") val includeAliases: Boolean? = false,
) {
  val isValid: Boolean
    get() = StringUtils.isNotBlank(firstName) || StringUtils.isNotBlank(surname) || dateOfBirth != null || StringUtils.isNotBlank(pncNumber) || StringUtils.isNotBlank(crn) ||
      StringUtils.isNotBlank(nomsNumber) || StringUtils.isNotBlank(croNumber)
}
