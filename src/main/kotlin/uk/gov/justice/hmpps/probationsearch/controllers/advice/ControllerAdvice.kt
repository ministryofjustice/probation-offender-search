package uk.gov.justice.hmpps.probationsearch.controllers.advice

import com.fasterxml.jackson.databind.JsonMappingException
import io.sentry.Sentry
import jakarta.validation.ConstraintViolationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import uk.gov.justice.hmpps.probationsearch.DataLoadFailureException
import uk.gov.justice.hmpps.probationsearch.IndexNotReadyException
import uk.gov.justice.hmpps.probationsearch.InvalidRequestException
import uk.gov.justice.hmpps.probationsearch.NotFoundException
import uk.gov.justice.hmpps.probationsearch.UnauthorisedException
import uk.gov.justice.hmpps.probationsearch.contactsearch.ContactSearchController
import uk.gov.justice.hmpps.probationsearch.controllers.OffenderSearchController

@RestControllerAdvice(basePackageClasses = [OffenderSearchController::class, ContactSearchController::class])
class ControllerAdvice {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @ExceptionHandler(RestClientResponseException::class)
  fun handleException(e: RestClientResponseException): ResponseEntity<ByteArray> {
    log.error("Unexpected exception", e)
    Sentry.captureException(e)
    return ResponseEntity
      .status(e.statusCode)
      .body(e.responseBodyAsByteArray)
  }

  @ExceptionHandler(RestClientException::class)
  fun handleException(e: RestClientException): ResponseEntity<ErrorResponse> {
    log.error("Unexpected exception", e)
    Sentry.captureException(e)
    return ResponseEntity
      .status(HttpStatus.INTERNAL_SERVER_ERROR)
      .body(ErrorResponse(status = HttpStatus.INTERNAL_SERVER_ERROR.value(), developerMessage = e.message))
  }

  @ExceptionHandler(AccessDeniedException::class)
  fun handleException(e: AccessDeniedException?): ResponseEntity<ErrorResponse> {
    return ResponseEntity
      .status(HttpStatus.FORBIDDEN)
      .body(ErrorResponse(status = HttpStatus.FORBIDDEN.value()))
  }

  @ExceptionHandler(NotFoundException::class)
  fun handleException(e: NotFoundException): ResponseEntity<ErrorResponse> {
    return ResponseEntity
      .status(HttpStatus.NOT_FOUND)
      .body(ErrorResponse(status = HttpStatus.NOT_FOUND.value(), developerMessage = e.message))
  }

  @ExceptionHandler(UnauthorisedException::class)
  fun handleException(e: UnauthorisedException): ResponseEntity<ErrorResponse> {
    return ResponseEntity
      .status(HttpStatus.UNAUTHORIZED)
      .body(ErrorResponse(status = HttpStatus.UNAUTHORIZED.value(), developerMessage = e.message))
  }

  @ExceptionHandler(InvalidRequestException::class)
  fun handleException(e: InvalidRequestException): ResponseEntity<ErrorResponse> {
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .body(ErrorResponse(status = HttpStatus.BAD_REQUEST.value(), developerMessage = e.message))
  }

  @ExceptionHandler(uk.gov.justice.hmpps.probationsearch.BadRequestException::class)
  fun handleException(e: uk.gov.justice.hmpps.probationsearch.BadRequestException): ResponseEntity<ErrorResponse> {
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .body(ErrorResponse(status = HttpStatus.BAD_REQUEST.value(), developerMessage = e.message))
  }

  @ExceptionHandler(JsonMappingException::class)
  fun handleException(e: JsonMappingException): ResponseEntity<ErrorResponse> {
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .body(ErrorResponse(status = HttpStatus.BAD_REQUEST.value(), developerMessage = e.message))
  }

  @ExceptionHandler(MethodArgumentNotValidException::class)
  fun handleException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .body(ErrorResponse(status = HttpStatus.BAD_REQUEST.value(), developerMessage = e.developerMessage()))
  }

  @ExceptionHandler(ConstraintViolationException::class)
  fun onValidationError(ex: Exception): ResponseEntity<String> {
    return ResponseEntity<String>(HttpStatus.BAD_REQUEST)
  }

  @ExceptionHandler(IndexNotReadyException::class)
  fun handleException(e: IndexNotReadyException): ResponseEntity<ErrorResponse> {
    log.error("Unexpected exception", e)
    Sentry.captureException(e)
    return ResponseEntity
      .status(HttpStatus.SERVICE_UNAVAILABLE)
      .header(HttpHeaders.RETRY_AFTER, "30")
      .body(
        ErrorResponse(
          status = HttpStatus.SERVICE_UNAVAILABLE.value(),
          developerMessage = e.message,
          userMessage = "Indexing in progress. Please try again later.",
        ),
      )
  }

  @ExceptionHandler(DataLoadFailureException::class)
  fun handleException(e: DataLoadFailureException): ResponseEntity<ErrorResponse> {
    log.error("Unexpected exception", e)
    Sentry.captureException(e)
    return ResponseEntity
      .status(HttpStatus.INTERNAL_SERVER_ERROR)
      .body(ErrorResponse(status = HttpStatus.INTERNAL_SERVER_ERROR.value(), developerMessage = e.message))
  }
}

private fun MethodArgumentNotValidException.developerMessage(): String {
  return this.bindingResult.allErrors.joinToString { it.defaultMessage ?: "unknown" }
}
