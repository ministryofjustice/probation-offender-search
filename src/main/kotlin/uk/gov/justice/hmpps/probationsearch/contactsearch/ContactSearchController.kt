package uk.gov.justice.hmpps.probationsearch.contactsearch

import com.microsoft.applicationinsights.TelemetryClient
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.CompletableFuture.runAsync
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("/search/contacts")
class ContactSearchController(
  val contactSearchService: ContactSearchService,
  val telemetryClient: TelemetryClient,
  val virtualThreadExecutor: ExecutorService,
) {
  @PreAuthorize("hasAnyRole('ROLE_PROBATION_CONTACT_SEARCH', 'ROLE_PROBATION_INTEGRATION_ADMIN')")
  @RequestMapping(method = [RequestMethod.GET, RequestMethod.POST])
  fun searchContact(
    @RequestBody request: ContactSearchRequest,
    @ParameterObject @PageableDefault pageable: Pageable,
    @RequestParam(defaultValue = "false") semantic: Boolean = false,
  ): ContactSearchResponse {
    contactSearchService.audit(request, pageable)
    return if (semantic) {
      contactSearchService.semanticSearch(request, pageable)
    } else {
      runAsync({ semanticSearch(request, pageable) }, virtualThreadExecutor)
        .orTimeout(30, TimeUnit.SECONDS)
        .exceptionally {
          telemetryClient.trackException(RuntimeException(it))
          null
        }
      contactSearchService.keywordSearch(request, pageable)
    }
  }

  private fun semanticSearch(request: ContactSearchRequest, pageable: Pageable) {
    val started = Instant.now()
    val response = contactSearchService.semanticSearch(request, pageable)
    telemetryClient.trackEvent(
      "SemanticSearchCompleted",
      mapOf(
        "crn" to request.crn,
        "query" to request.query.length.toString(),
        "resultCount" to response.totalResults.toString(),
      ),
      mapOf(
        "duration" to started.until(Instant.now(), ChronoUnit.MILLIS).toDouble(),
      ),
    )
  }
}
