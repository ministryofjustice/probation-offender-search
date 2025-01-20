package uk.gov.justice.hmpps.probationsearch.contactsearch.activitysearch

import java.time.LocalDate
import java.time.ZonedDateTime

data class ActivitySearchRequest(
  val crn: String,
  val keywords: String? = "",
  val dateFrom: LocalDate? = null,
  val dateTo: LocalDate? = null,
  val filters: List<String> = emptyList(),
)

data class ActivitySearchAuditRequest(
  val search: ActivitySearchRequest,
  val username: String,
  val pagination: PageRequest,
  val dateTime: ZonedDateTime = ZonedDateTime.now(),
) {
  data class PageRequest(
    val page: Int,
    val pageSize: Int,
    val sort: String?,
    val direction: String?,
  )
}