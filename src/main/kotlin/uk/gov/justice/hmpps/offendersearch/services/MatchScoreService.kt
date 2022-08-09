package uk.gov.justice.hmpps.offendersearch.services

import com.fasterxml.jackson.annotation.JsonInclude
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.gov.justice.hmpps.offendersearch.dto.MatchRequest
import uk.gov.justice.hmpps.offendersearch.dto.OffenderMatch
import java.time.format.DateTimeFormatter

@Service
class MatchScoreService(@Qualifier("hmppsPersonMatchScoreWebClient") private val webClient: WebClient) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  internal fun scoreAll(matches: List<OffenderMatch>, matchRequest: MatchRequest): List<OffenderMatch> {
    return Flux.fromIterable(matches)
      .flatMap {
        score(matchRequest, it)
      }
      .collectList()
      .blockOptional()
      .orElseGet {
        if (matches.isNotEmpty())
          log.warn("Could not retrieve any probability scores from hmpps-person-match-score. Probability scores will be omitted for these matches.")
        matches
      }
      .sortedByDescending { it.matchProbability }
  }

  private fun score(matchRequest: MatchRequest, offenderMatch: OffenderMatch): Mono<OffenderMatch> {
    return webClient.post()
      .uri("/match")
      .bodyValue(matchRequest combinedIntoScoreRequestWith offenderMatch)
      .retrieve()
      .bodyToMono(MatchScoreResponse::class.java)
      .map {
        offenderMatch.copy(
          matchProbability = it.match_probability.`0`
        )
      }
      .onErrorResume {
        log.warn("There was an error retrieving probability scores from hmpps-person-match-score. The probability score will be omitted for this match.", it)
        Mono.just(offenderMatch)
      }
  }
}


private infix fun MatchRequest.combinedIntoScoreRequestWith(offenderMatch: OffenderMatch): MatchScoreRequest {
  return MatchScoreRequest(
    unique_id = ValuePair("1", "2"),
    first_name = ValuePair(this.firstName, offenderMatch.offender.firstName),
    surname = ValuePair(this.surname, offenderMatch.offender.surname),
    dob = ValuePair(
      this.dateOfBirth?.format(DateTimeFormatter.ISO_DATE),
      offenderMatch.offender.dateOfBirth?.format(DateTimeFormatter.ISO_DATE)
    ),
    pnc_number = ValuePair(this.pncNumber, offenderMatch.offender.otherIds?.pncNumber),
    source_dataset = ValuePair("LIBRA", "DELIUS")
  )
}

private data class MatchScoreRequest(
  val unique_id: ValuePair,
  val first_name: ValuePair,
  val surname: ValuePair,
  val dob: ValuePair,
  val pnc_number: ValuePair,
  val source_dataset: ValuePair
)

private data class MatchScoreResponse(
  val match_probability: ValueDouble
)

@JsonInclude(JsonInclude.Include.NON_NULL)
private data class ValuePair(
  val `0`: String?,
  val `1`: String?
)

private data class ValueDouble(
  val `0`: Double
)
