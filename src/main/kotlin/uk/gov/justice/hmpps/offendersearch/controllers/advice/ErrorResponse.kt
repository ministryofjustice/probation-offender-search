package uk.gov.justice.hmpps.offendersearch.controllers.advice

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponse(
  @Schema(required = true, description = "Status of Error Code", example = "400")
  val status: Int,
  @Schema(required = false, description = "Developer Information message", example = "System is down")
  val developerMessage: String? = null,
  @Schema(required = true, description = "Internal Error Code", example = "20012") val errorCode: Int? = null,
  @Schema(required = true, description = "Error message information", example = "Offender Not Found") val userMessage: String? = null,
  @Schema(required = false, description = "Additional information about the error", example = "Hard disk failure") val moreInfo: String? = null
)
