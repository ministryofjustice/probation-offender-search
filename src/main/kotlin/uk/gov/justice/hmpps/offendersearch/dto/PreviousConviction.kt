package uk.gov.justice.hmpps.offendersearch.dto

import java.time.LocalDate

data class PreviousConviction(
  val convictionDate: LocalDate? = null,
  val detail: Map<String, String>? = null
)
