package uk.gov.justice.hmpps.offendersearch.cvlsearch

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@RestControllerAdvice
class RestExceptionHandler : ResponseEntityExceptionHandler() {
  override fun handleMethodArgumentNotValid(
    ex: MethodArgumentNotValidException,
    headers: HttpHeaders,
    status: HttpStatus,
    request: WebRequest
  ): ResponseEntity<Any> {
    val errors = ErrorsResponse(
      (request as ServletWebRequest).request.requestURI,
      ex.bindingResult.fieldErrors.map { FieldError(it.field, it.defaultMessage) },
      HttpStatus.BAD_REQUEST
    )
    println(ex)
    return ResponseEntity(errors, errors.status)
  }
}

class ErrorsResponse(val path: String, val fieldErrors: List<FieldError>, val status: HttpStatus)

data class FieldError(
  val field: String?,
  val message: String?,
)
