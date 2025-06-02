package uk.gov.justice.hmpps.probationsearch.contactsearch

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.hmpps.probationsearch.services.FeatureFlags
import uk.gov.justice.hmpps.probationsearch.utils.TermSplitter
import java.time.Instant
import java.time.temporal.ChronoUnit

@RestController
@RequestMapping("/search/contacts")
class ContactSearchController(
  private val contactSearchService: ContactSearchService,
  private val telemetryClient: TelemetryClient,
  private val featureFlags: FeatureFlags,
) {
  @PreAuthorize("hasAnyRole('ROLE_PROBATION_CONTACT_SEARCH', 'ROLE_PROBATION_INTEGRATION_ADMIN')")
  @RequestMapping(method = [RequestMethod.GET, RequestMethod.POST])
  suspend fun searchContact(
    @Valid @RequestBody request: ContactSearchRequest,
    @ParameterObject @PageableDefault pageable: Pageable,
    @RequestParam(required = false) semantic: Boolean? = null,
  ): ContactSearchResponse {
    val useSemanticSearch = semantic ?: featureFlags.enabled(FeatureFlags.SEMANTIC_CONTACT_SEARCH)
    return if (useSemanticSearch) {
      val started = Instant.now()
      val response = contactSearchService.semanticSearch(request, pageable)
      telemetryClient.trackEvent(
        "SemanticSearchCompleted",
        mapOf(
          "crn" to request.crn,
          "query" to request.query.length.toString(),
          "resultCount" to response.totalResults.toString(),
          "queryTermCount" to TermSplitter.split(request.query).size.toString(),
          "page" to pageable.pageNumber.toString(),
          "resultCountForPage" to response.results.size.toString(),
          "semanticOnlyResultCountForPage" to response.results.count { it.highlights.isEmpty() }.toString()
        ),
        mapOf(
          "duration" to started.until(Instant.now(), ChronoUnit.MILLIS).toDouble(),
        ),
      )
      response
    } else {
      contactSearchService.keywordSearch(request, pageable)
    }
  }
}
