package uk.gov.justice.hmpps.probationsearch.contactsearch

import com.microsoft.applicationinsights.TelemetryClient
import io.sentry.Sentry
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.hmpps.probationsearch.contactsearch.audit.ContactSearchAuditService
import uk.gov.justice.hmpps.probationsearch.contactsearch.extensions.AsyncExtensions.runAsync
import uk.gov.justice.hmpps.probationsearch.contactsearch.keyword.ContactKeywordSearchService
import uk.gov.justice.hmpps.probationsearch.contactsearch.model.ContactSearchRequest
import uk.gov.justice.hmpps.probationsearch.contactsearch.model.ContactSearchResponse
import uk.gov.justice.hmpps.probationsearch.contactsearch.semantic.ContactSemanticSearchService
import uk.gov.justice.hmpps.probationsearch.services.FeatureFlags

@RestController
@RequestMapping("/search/contacts")
class ContactSearchController(
  private val semanticSearchService: ContactSemanticSearchService,
  private val keywordSearchService: ContactKeywordSearchService,
  private val auditService: ContactSearchAuditService,
  private val telemetryClient: TelemetryClient,
  private val featureFlags: FeatureFlags,
  @Qualifier("applicationTaskExecutor")
  private val executor: SimpleAsyncTaskExecutor,
) {
  @PreAuthorize("hasAnyRole('ROLE_PROBATION_CONTACT_SEARCH', 'ROLE_PROBATION_INTEGRATION_ADMIN')")
  @RequestMapping(method = [RequestMethod.GET, RequestMethod.POST])
  fun searchContact(
    @Valid @RequestBody request: ContactSearchRequest,
    @ParameterObject @PageableDefault pageable: Pageable,
    @RequestParam(required = false) semantic: Boolean? = null,
  ): ContactSearchResponse {
    auditService.audit(request, pageable)

    val useSemanticSearch = semantic ?: featureFlags.enabled(FeatureFlags.SEMANTIC_CONTACT_SEARCH)
    return if (useSemanticSearch) {
      semanticSearchService.search(request, pageable)
    } else {
      callSemanticSearchInBackground(request, pageable)
      keywordSearchService.search(request, pageable)
    }
  }

  fun callSemanticSearchInBackground(request: ContactSearchRequest, pageable: Pageable) {
    executor.runAsync {
      try {
        semanticSearchService.search(request, pageable)
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
  }
}
