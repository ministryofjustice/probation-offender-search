package uk.gov.justice.hmpps.probationsearch.services

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.awaitExchange
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import uk.gov.justice.hmpps.probationsearch.NotFoundException
import uk.gov.justice.hmpps.probationsearch.contactsearch.ContactJson
import uk.gov.justice.hmpps.probationsearch.contactsearch.ContactSearchAuditRequest
import uk.gov.justice.hmpps.probationsearch.contactsearch.activitysearch.ActivitySearchAuditRequest
import java.time.Duration

@Service
class DeliusService(@Qualifier("searchAndDeliusApiWebClient") private val webClient: WebClient) {
  suspend fun auditContactSearch(contactSearchAuditRequest: ContactSearchAuditRequest) {
    webClient.post()
      .uri("/probation-search/audit/contact-search")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(contactSearchAuditRequest)
      .awaitExchange {
        it.toBodilessEntity().retryWhen(Retry.backoff(3, Duration.ofMillis(200))).block()
      }
  }

  suspend fun auditActivitySearch(activitySearchAuditRequest: ActivitySearchAuditRequest) {
    webClient.post()
      .uri("/probation-search/audit/contact-search")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(activitySearchAuditRequest)
      .awaitExchange {
        it.toBodilessEntity().retryWhen(Retry.backoff(3, Duration.ofMillis(200))).block()
      }
  }

  fun getContacts(crn: String): List<ContactJson> = webClient.get()
    .uri("/case/$crn/contacts")
    .retrieve()
    .toEntityList(ContactJson::class.java)
    .onErrorResume(WebClientResponseException::class.java) {
      if (it.statusCode == NOT_FOUND) Mono.empty() else Mono.error(it)
    }
    .retryWhen(Retry.backoff(3, Duration.ofMillis(200)))
    .block()?.body ?: throw NotFoundException("Person with CRN of $crn not found")
}