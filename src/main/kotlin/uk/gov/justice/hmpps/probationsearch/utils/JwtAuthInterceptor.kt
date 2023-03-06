package uk.gov.justice.hmpps.probationsearch.utils

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse

class JwtAuthInterceptor : ClientHttpRequestInterceptor {
  override fun intercept(
    request: HttpRequest,
    body: ByteArray,
    execution: ClientHttpRequestExecution
  ): ClientHttpResponse {
    val headers = request.headers
    headers.add(HttpHeaders.AUTHORIZATION, UserContext.getAuthToken())
    return execution.execute(request, body)
  }
}
