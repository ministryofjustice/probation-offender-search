package uk.gov.justice.hmpps.offendersearch.contactsearch

import com.fasterxml.jackson.annotation.JsonAlias
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class ContactSearchRequest(
  val crn: String,
  val query: String,
  val matchAllTerms: Boolean = true,
  val sort: SortField = SortField.RELEVANCE,
  val sortDirection: SortDirection = SortDirection.DESCENDING
)

enum class SortField(name: String) {
  @JsonAlias("relevance")
  RELEVANCE("relevance"),

  @JsonAlias("lastUpdatedDateTime", "last_updated_datetime", "lastUpdated")
  LAST_UPDATED_DATETIME("lastUpdated"),

  @JsonAlias("contactDate", "contact_date")
  CONTACT_DATE("contactDate")
}

enum class SortDirection(name: String) {
  @JsonAlias("ascending", "asc")
  ASCENDING("asc"),

  @JsonAlias("descending", "desc")
  DESCENDING("desc")
}

data class SearchContactResponse(
  val size: Int,
  val page: Int,
  val totalResults: Long,
  val totalPages: Int,
  val results: List<Contact>
)

data class Contact(
  val id: Long,
  val typeCode: String,
  val typeDescription: String,
  val outcomeCode: String?,
  val outcomeDescription: String?,
  val description: String?,
  val notes: String?,
  val date: LocalDate,
  val startTime: LocalTime?,
  val endTime: LocalTime?,
  val lastUpdatedDateTime: LocalDateTime,
  val highlights: Map<String, List<String>>,
)
