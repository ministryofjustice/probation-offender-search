package uk.gov.justice.hmpps.probationsearch.contactsearch.model

import jakarta.validation.constraints.Size

class ContactSearchRequest(
  val crn: String,
  query: String? = "",
  val matchAllTerms: Boolean = true,
  includeScores: Boolean? = false,
) {
  @Size(max = 1000, message = "query length must not exceed 1000 characters")
  val query = query ?: ""
  val includeScores: Boolean = includeScores ?: false
}