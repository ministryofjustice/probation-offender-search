package uk.gov.justice.hmpps.probationsearch.dto

import java.time.LocalDate

data class Provision(
  val provisionId: Long? = null,
  val provisionType: KeyValue? = null,
  val category: KeyValue? = null,
  val startDate: LocalDate? = null,
  val endDate: LocalDate? = null,
  val notes: String? = null,
)
