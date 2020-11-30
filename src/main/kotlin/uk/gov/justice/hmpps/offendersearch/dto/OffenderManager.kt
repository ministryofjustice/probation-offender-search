package uk.gov.justice.hmpps.offendersearch.dto

import java.time.LocalDate

data class OffenderManager(
  val trustOfficer: Human? = null,
  val staff: StaffHuman? = null,
  val providerEmployee: Human? = null,
  val partitionArea: String? = null,
  val softDeleted: Boolean? = null,
  val team: Team? = null,
  val probationArea: ProbationArea? = null,
  val fromDate: LocalDate? = null,
  val toDate: LocalDate? = null,
  val active: Boolean? = null,
  val allocationReason: KeyValue? = null
)
