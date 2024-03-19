package uk.gov.justice.hmpps.probationsearch.services

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitExchangeOrNull
import reactor.util.retry.Retry
import uk.gov.justice.hmpps.probationsearch.contactsearch.ContactSearchAuditRequest
import java.time.Duration

@Service
class DeliusService(@Qualifier("searchAndDeliusApiWebClient") private val webClient: WebClient) {
  suspend fun auditContactSearch(contactSearchAuditRequest: ContactSearchAuditRequest) {
    webClient.post()
      .uri("/audit/contact-search")
      .bodyValue(contactSearchAuditRequest)
      .awaitExchangeOrNull {
        it.toBodilessEntity().retryWhen(Retry.backoff(3, Duration.ofMillis(200))).onErrorComplete().subscribe()
      }
  }
}