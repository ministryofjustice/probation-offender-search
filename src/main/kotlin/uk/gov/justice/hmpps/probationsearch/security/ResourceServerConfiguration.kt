@file:Suppress("DEPRECATION")

package uk.gov.justice.hmpps.probationsearch.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class ResourceServerConfiguration {
  @Bean
  fun filterChain(http: HttpSecurity): SecurityFilterChain {
    http
      .sessionManagement()
      .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      .and().csrf().disable()
      .authorizeHttpRequests {
        it
          .requestMatchers(
            "/webjars/**", "/favicon.ico", "/csrf",
            "/health/**", "/info",
            "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui/**",
            "/swagger-resources", "/swagger-resources/configuration/ui", "/swagger-resources/configuration/security",
            "/synthetic-monitor" // This endpoint is secured in the ingress rather than the app so that it can be called from within the namespace without requiring authentication
          ).permitAll()
          .anyRequest().authenticated()
      }.oauth2ResourceServer().jwt().jwtAuthenticationConverter(AuthAwareTokenConverter())
    return http.build()
  }
}
