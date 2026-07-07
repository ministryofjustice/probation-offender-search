package uk.gov.justice.hmpps.probationsearch.contactsearch.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDate

class ContactSearchRequest(
  val crn: String,
  query: String? = "",
  val matchAllTerms: Boolean = true,
  includeScores: Boolean? = false,
  val dateFrom: LocalDate? = null,
  val dateTo: LocalDate? = null,
  @Schema(
    description = "Whether to include system generated contacts in the search results. Defaults to true.",
    example = "false",
  )
  val includeSystemGenerated: Boolean = true,
  val filters: List<String> = emptyList(),
  val typeCodes: List<String> = emptyList(),
) {
  @Size(max = 1000, message = "query length must not exceed 1000 characters")
  val query = query ?: ""
  val includeScores: Boolean = includeScores ?: false
}