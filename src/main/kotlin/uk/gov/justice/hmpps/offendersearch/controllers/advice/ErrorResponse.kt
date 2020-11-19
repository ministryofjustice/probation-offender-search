package uk.gov.justice.hmpps.offendersearch.controllers.advice

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.annotations.ApiModelProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponse(
  @ApiModelProperty(required = true, value = "Status of Error Code", example = "400", position = 0)
  val status: Int,
  @ApiModelProperty(required = false, value = "Developer Information message", example = "System is down", position = 3)
  val developerMessage: String? = null,
  @ApiModelProperty(required = true, value = "Internal Error Code", example = "20012", position = 1) val errorCode: Int? = null,
  @ApiModelProperty(required = true, value = "Error message information", example = "Offender Not Found", position = 2) val userMessage: String? = null,
  @ApiModelProperty(required = false, value = "Additional information about the error", example = "Hard disk failure", position = 4) val moreInfo: String? = null
)
