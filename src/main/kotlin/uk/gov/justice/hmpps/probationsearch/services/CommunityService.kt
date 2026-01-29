package uk.gov.justice.hmpps.probationsearch.services

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

@Service
class CommunityService(
  @Qualifier("communityApiWebClient") private val webClient: WebClient,
  private val objectMapper: ObjectMapper,
) {
  fun canAccessOffender(crn: String): AccessLimitation =
    webClient.get()
      .uri("/secure/offenders/crn/$crn/userAccess")
      .retrieve()
      .bodyToMono(AccessLimitation::class.java)
      .onErrorResume(::noAccessIfNotFound)
      .onErrorResume(::noAccessIfForbidden)
      .block()!!

  private fun noAccessIfNotFound(exception: Throwable): Mono<out AccessLimitation> {
    return if (exception is WebClientResponseException.NotFound) {
      Mono.just(
        AccessLimitation(
          userRestricted = true,
          userExcluded = true,
        ),
      )
    } else {
      Mono.error(exception)
    }
  }

  private fun noAccessIfForbidden(exception: Throwable): Mono<out AccessLimitation> {
    return if (exception is WebClientResponseException.Forbidden) {
      Mono.just(
        objectMapper.readValue<AccessLimitation>(exception.responseBodyAsString),
      )
    } else {
      Mono.error(exception)
    }
  }
}

data class AccessLimitation(
  val userRestricted: Boolean,
  val userExcluded: Boolean,
)
