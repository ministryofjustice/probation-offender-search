package uk.gov.justice.hmpps.probationsearch.config

import io.flipt.client.FliptClient
import io.flipt.client.models.ClientTokenAuthentication
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
    FliptClient.builder().namespace(NAMESPACE).url(url)
      .authentication(ClientTokenAuthentication(token)).build()

  companion object {
    const val NAMESPACE = "probation-integration"
  }
}