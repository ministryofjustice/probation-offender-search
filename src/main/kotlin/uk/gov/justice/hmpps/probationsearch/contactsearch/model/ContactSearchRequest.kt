package uk.gov.justice.hmpps.probationsearch.contactsearch.model

import jakarta.validation.constraints.Size
import java.time.LocalDate

class ContactSearchRequest(
  val crn: String,
  query: String? = "",
  val matchAllTerms: Boolean = true,
  includeScores: Boolean? = false,
  val dateFrom: LocalDate? = null,
  val dateTo: LocalDate? = null,
  val includeSystemGenerated: Boolean = false,
  val filters: List<String> = emptyList(),
) {
  @Size(max = 1000, message = "query length must not exceed 1000 characters")
  val query = query ?: ""
  val includeScores: Boolean = includeScores ?: false
}