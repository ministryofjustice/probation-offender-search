package uk.gov.justice.hmpps.probationsearch.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.hmpps.kotlin.auth.dsl.ResourceServerConfigurationCustomizer

@Configuration
class ResourceServerConfiguration {
  @Bean
  fun resourceServerCustomizer() = ResourceServerConfigurationCustomizer {
    unauthorizedRequestPaths {
      addPaths = setOf(
        "/webjars/**", "/favicon.ico", "/csrf",
        "/health/**", "/info",
        "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui/**",
        "/swagger-resources", "/swagger-resources/configuration/ui", "/swagger-resources/configuration/security",
      )
    }
  }
}
