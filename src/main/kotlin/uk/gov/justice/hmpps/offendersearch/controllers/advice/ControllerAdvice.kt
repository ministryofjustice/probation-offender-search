package uk.gov.justice.hmpps.offendersearch.controllers.advice

import com.fasterxml.jackson.databind.JsonMappingException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import uk.gov.justice.hmpps.offendersearch.BadRequestException
import uk.gov.justice.hmpps.offendersearch.InvalidRequestException
import uk.gov.justice.hmpps.offendersearch.NotFoundException
import uk.gov.justice.hmpps.offendersearch.UnauthorisedException
import uk.gov.justice.hmpps.offendersearch.controllers.OffenderSearchController
import javax.validation.ConstraintViolationException

@RestControllerAdvice(basePackageClasses = [OffenderSearchController::class])
class ControllerAdvice {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @ExceptionHandler(RestClientResponseException::class)
  fun handleException(e: RestClientResponseException): ResponseEntity<ByteArray> {
    log.error("Unexpected exception", e)
    return ResponseEntity
      .status(e.rawStatusCode)
      .body(e.responseBodyAsByteArray)
  }

  @ExceptionHandler(RestClientException::class)
  fun handleException(e: RestClientException): ResponseEntity<ErrorResponse> {
    log.error("Unexpected exception", e)
    return ResponseEntity
      .status(HttpStatus.INTERNAL_SERVER_ERROR)
      .body(ErrorResponse(status = HttpStatus.INTERNAL_SERVER_ERROR.value(), developerMessage = e.message))
  }

  @ExceptionHandler(AccessDeniedException::class)
  fun handleException(e: AccessDeniedException?): ResponseEntity<ErrorResponse> {
    log.debug("Forbidden (403) returned", e)
    return ResponseEntity
      .status(HttpStatus.FORBIDDEN)
      .body(ErrorResponse(status = HttpStatus.FORBIDDEN.value()))
  }

  @ExceptionHandler(NotFoundException::class)
  fun handleException(e: NotFoundException): ResponseEntity<ErrorResponse> {
    log.debug("Not Found (404) returned", e)
    return ResponseEntity
      .status(HttpStatus.NOT_FOUND)
      .body(ErrorResponse(status = HttpStatus.NOT_FOUND.value(), developerMessage = e.message))
  }

  @ExceptionHandler(UnauthorisedException::class)
  fun handleException(e: UnauthorisedException): ResponseEntity<ErrorResponse> {
    log.debug("Unauthorised (401) returned", e)
    return ResponseEntity
      .status(HttpStatus.UNAUTHORIZED)
      .body(ErrorResponse(status = HttpStatus.UNAUTHORIZED.value(), developerMessage = e.message))
  }

  @ExceptionHandler(InvalidRequestException::class)
  fun handleException(e: InvalidRequestException): ResponseEntity<ErrorResponse> {
    log.debug("Bad Request (400) returned", e)
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .body(ErrorResponse(status = HttpStatus.BAD_REQUEST.value(), developerMessage = e.message))
  }

  @ExceptionHandler(BadRequestException::class)
  fun handleException(e: BadRequestException): ResponseEntity<ErrorResponse> {
    log.debug("Bad request (400) returned", e)
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .body(ErrorResponse(status = HttpStatus.BAD_REQUEST.value(), developerMessage = e.message))
  }

  @ExceptionHandler(JsonMappingException::class)
  fun handleException(e: JsonMappingException): ResponseEntity<ErrorResponse> {
    log.debug("Bad request (400) returned", e)
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .body(ErrorResponse(status = HttpStatus.BAD_REQUEST.value(), developerMessage = e.message))
  }

  @ExceptionHandler(MethodArgumentNotValidException::class)
  fun handleException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
    log.debug("Bad request (400) returned", e)
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .body(ErrorResponse(status = HttpStatus.BAD_REQUEST.value(), developerMessage = e.developerMessage()))
  }

  @ExceptionHandler(ConstraintViolationException::class)
  fun onValidationError(ex: Exception): ResponseEntity<String> {
    return ResponseEntity<String>(HttpStatus.BAD_REQUEST)
  }
}

private fun MethodArgumentNotValidException.developerMessage(): String {
  return this.bindingResult.allErrors.joinToString { it.defaultMessage ?: "unknown" }
}
