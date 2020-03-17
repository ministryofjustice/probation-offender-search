@file:Suppress("DEPRECATION")

package uk.gov.justice.hmpps.offendersearch.security


import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import springfox.documentation.swagger2.annotations.EnableSwagger2

@Configuration
@EnableSwagger2
@EnableWebSecurity
class ResourceServerConfiguration : WebSecurityConfigurerAdapter() {
  override fun configure(http: HttpSecurity) {
    http
        .sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and().csrf().disable()
        .authorizeRequests { auth ->
          auth.antMatchers("/webjars/**", "/favicon.ico", "/csrf",
              "/health", "/health/ping", "/info", "/ping",
              "/v2/api-docs",
              "/swagger-ui.html", "/swagger-resources", "/swagger-resources/configuration/ui",
              "/swagger-resources/configuration/security")
              .permitAll().anyRequest().authenticated()
        }.oauth2ResourceServer().jwt().jwtAuthenticationConverter(AuthAwareTokenConverter())
  }
}