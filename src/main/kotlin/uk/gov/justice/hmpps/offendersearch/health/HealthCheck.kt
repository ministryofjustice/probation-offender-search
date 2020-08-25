package uk.gov.justice.hmpps.offendersearch.health

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.time.Duration


abstract class HealthCheck(private val webClient: WebClient, private val timeout: Duration) : HealthIndicator {

  override fun health(): Health? {
    return webClient.get()
        .uri("/health/ping")
        .retrieve()
        .toEntity(String::class.java)
        .flatMap { Mono.just(Health.up().withDetail("HttpStatus", it?.statusCode).build()) }
        .onErrorResume(WebClientResponseException::class.java) { Mono.just(Health.down(it).withDetail("body", it.responseBodyAsString).withDetail("HttpStatus", it.statusCode).build()) }
        .onErrorResume(Exception::class.java) { Mono.just(Health.down(it).build()) }
        .block(timeout)
  }
}

@Component
class CommunityApiHealth
constructor(@Qualifier("communityApiHealthWebClient") webClient: WebClient, @Value("\${api.health-timeout:2s}") timeout: Duration) : HealthCheck(webClient, timeout)

@Component
class OAuthApiHealth
constructor(@Qualifier("oauthApiHealthWebClient") webClient: WebClient, @Value("\${api.health-timeout:2s}") timeout: Duration) : HealthCheck(webClient, timeout)

