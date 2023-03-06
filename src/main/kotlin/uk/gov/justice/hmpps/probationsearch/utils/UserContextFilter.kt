package uk.gov.justice.hmpps.probationsearch.utils

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.FilterConfig
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component

@Component
@Order(4)
class UserContextFilter : Filter {
  override fun doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain) {
    val httpServletRequest = servletRequest as HttpServletRequest
    val authToken: String? = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION)
    UserContext.setAuthToken(authToken)
    filterChain.doFilter(httpServletRequest, servletResponse)
  }

  override fun init(filterConfig: FilterConfig) {}
  override fun destroy() {}
}
