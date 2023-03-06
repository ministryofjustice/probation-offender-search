package uk.gov.justice.hmpps.probationsearch.dto

import java.time.LocalDate

data class Disability(
  val disabilityId: Long? = null,
  val disabilityType: KeyValue? = null,
  val startDate: LocalDate? = null,
  val endDate: LocalDate? = null,
  val notes: String? = null
)
