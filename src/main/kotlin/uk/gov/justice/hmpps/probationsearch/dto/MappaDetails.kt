package uk.gov.justice.hmpps.probationsearch.dto

import java.time.LocalDate

data class MappaDetails(
  val level: Int? = null,
  val levelDescription: String? = null,
  val category: Int? = null,
  val categoryDescription: String? = null,
  val startDate: LocalDate? = null,
  val reviewDate: LocalDate? = null,
  val team: KeyValue? = null,
  val officer: StaffHuman? = null,
  val probationArea: KeyValue? = null,
  val notes: String? = null,
)
