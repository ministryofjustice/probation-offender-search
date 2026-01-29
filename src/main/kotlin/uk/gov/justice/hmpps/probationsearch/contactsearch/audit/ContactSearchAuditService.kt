package uk.gov.justice.hmpps.probationsearch.contactsearch.audit

import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.Context
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.springframework.data.domain.Pageable
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.hmpps.probationsearch.config.SecurityCoroutineContext
import uk.gov.justice.hmpps.probationsearch.contactsearch.activitysearch.ActivitySearchAuditRequest
import uk.gov.justice.hmpps.probationsearch.contactsearch.activitysearch.ActivitySearchRequest
import uk.gov.justice.hmpps.probationsearch.contactsearch.activitysearch.ActivitySearchService
import uk.gov.justice.hmpps.probationsearch.contactsearch.extensions.OpenSearchRestClientExtensions.fieldSorts
import uk.gov.justice.hmpps.probationsearch.contactsearch.model.ContactSearchRequest
import uk.gov.justice.hmpps.probationsearch.contactsearch.model.SortType
import uk.gov.justice.hmpps.probationsearch.services.DeliusService
import uk.gov.justice.hmpps.sqs.audit.HmppsAuditService
import java.time.Instant

@Service
class ContactSearchAuditService(
  private val objectMapper: ObjectMapper,
  private val auditService: HmppsAuditService?,
  private val deliusService: DeliusService,
) {
  fun audit(request: ContactSearchRequest, pageable: Pageable) {
    // Audit service requires coroutines
    CoroutineScope(Context.current().asContextElement() + SecurityCoroutineContext()).launch {
      auditService?.publishEvent(
        what = "Search Contacts",
        who = SecurityContextHolder.getContext().authentication!!.name,
        `when` = Instant.now(),
        subjectId = request.crn,
        subjectType = "CRN",
        correlationId = Span.current().spanContext.traceId,
        service = "probation-search",
        details = objectMapper.writeValueAsString(request),
      )

      val fieldSorts = pageable.sort.fieldSorts()
      deliusService.auditContactSearch(
        ContactSearchAuditRequest(
          request,
          SecurityContextHolder.getContext().authentication!!.name,
          ContactSearchAuditRequest.PageRequest(
            pageable.pageNumber,
            pageable.pageSize,
            fieldSorts.mapNotNull { SortType.from(it.fieldName)?.aliases?.first() }.joinToString(),
            fieldSorts.joinToString { it.order().toString() },
          ),
        ),
      )
    }
  }

  fun audit(request: ActivitySearchRequest, pageable: Pageable) {
    CoroutineScope(Context.current().asContextElement() + SecurityCoroutineContext()).launch {
      auditService?.publishEvent(
        what = "Search Contacts for Activity Log",
        who = SecurityContextHolder.getContext().authentication!!.name,
        `when` = Instant.now(),
        subjectId = request.crn,
        subjectType = "CRN",
        correlationId = Span.current().spanContext.traceId,
        service = "probation-search",
        details = objectMapper.writeValueAsString(request),
      )

      val fieldSorts = pageable.sort.fieldSorts()
      deliusService.auditActivitySearch(
        ActivitySearchAuditRequest(
          request,
          SecurityContextHolder.getContext().authentication!!.name,
          ActivitySearchAuditRequest.PageRequest(
            pageable.pageNumber,
            pageable.pageSize,
            fieldSorts.mapNotNull { ActivitySearchService.SortType.from(it.fieldName)?.aliases?.first() }
              .joinToString(),
            fieldSorts.joinToString { it.order().toString() },
          ),
        ),
      )
    }
  }
}
