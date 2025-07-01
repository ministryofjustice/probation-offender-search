package uk.gov.justice.hmpps.probationsearch.config

import io.flipt.api.FliptClient
import io.flipt.api.FliptClient.builder
import io.flipt.api.authentication.ClientTokenAuthenticationStrategy
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty("flipt.url")
class FliptConfig(
  @param:Value($$"${flipt.url}") private val url: String,
  @param:Value($$"${flipt.token}") private val token: String,
) {
  @Bean
  fun fliptApiClient(): FliptClient =
    builder().url(url).authentication(ClientTokenAuthenticationStrategy(token)).build()

  companion object {
    const val NAMESPACE = "probation-integration"
  }
}