package uk.gov.justice.hmpps.probationsearch.contactsearch.audit

import uk.gov.justice.hmpps.probationsearch.contactsearch.model.ContactSearchRequest
import java.time.ZonedDateTime

data class ContactSearchAuditRequest(
  val search: ContactSearchRequest,
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