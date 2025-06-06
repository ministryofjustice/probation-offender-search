package uk.gov.justice.hmpps.probationsearch.contactsearch

import com.microsoft.applicationinsights.TelemetryClient
import io.opentelemetry.context.Context
import io.opentelemetry.extension.kotlin.asContextElement
import io.sentry.Sentry
import jakarta.validation.Valid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.hmpps.probationsearch.config.SecurityCoroutineContext
import uk.gov.justice.hmpps.probationsearch.contactsearch.audit.ContactSearchAuditService
import uk.gov.justice.hmpps.probationsearch.contactsearch.keyword.ContactKeywordSearchService
import uk.gov.justice.hmpps.probationsearch.contactsearch.model.ContactSearchRequest
import uk.gov.justice.hmpps.probationsearch.contactsearch.model.ContactSearchResponse
import uk.gov.justice.hmpps.probationsearch.contactsearch.semantic.ContactSemanticSearchService
import uk.gov.justice.hmpps.probationsearch.services.FeatureFlags
import uk.gov.justice.hmpps.probationsearch.utils.TermSplitter
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTimedValue
import kotlin.time.toJavaDuration

@RestController
@RequestMapping("/search/contacts")
class ContactSearchController(
  private val semanticSearchService: ContactSemanticSearchService,
  private val keywordSearchService: ContactKeywordSearchService,
  private val auditService: ContactSearchAuditService,
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
    auditService.audit(request, pageable)

    val useSemanticSearch = semantic ?: featureFlags.enabled(FeatureFlags.SEMANTIC_CONTACT_SEARCH)
    return if (useSemanticSearch) {
      val (response, duration) = measureTimedValue { semanticSearchService.search(request, pageable) }
      trackSemanticSearch(request, response, pageable, duration)
      response
    } else {
      CoroutineScope(Dispatchers.IO).launch(Context.current().asContextElement() + SecurityCoroutineContext()) {
        try {
          val (response, duration) = measureTimedValue {
            withTimeout(30.seconds) { semanticSearchService.search(request, pageable) }
          }
          trackSemanticSearch(request, response, pageable, duration)
        } catch (e: Exception) {
          telemetryClient.trackEvent(
            "SemanticSearchFailed",
            mapOf("crn" to request.crn, "query" to request.query.length.toString(), "reason" to e.message),
            null,
          )
          telemetryClient.trackException(e)
          Sentry.captureException(e)
        }
      }
      keywordSearchService.search(request, pageable)
    }
  }

  private fun trackSemanticSearch(
    request: ContactSearchRequest,
    response: ContactSearchResponse,
    pageable: Pageable,
    duration: Duration,
  ) {
    telemetryClient.trackEvent(
      "SemanticSearchCompleted",
      mapOf(
        "crn" to request.crn,
        "query" to request.query.length.toString(),
        "resultCount" to response.totalResults.toString(),
        "queryTermCount" to TermSplitter.split(request.query).size.toString(),
        "page" to pageable.pageNumber.toString(),
        "resultCountForPage" to response.results.size.toString(),
        "semanticOnlyResultCountForPage" to response.results.count { it.semanticMatch }.toString(),
      ),
      mapOf(
        "duration" to duration.toJavaDuration().toMillis().toDouble(),
      ),
    )
  }
}
