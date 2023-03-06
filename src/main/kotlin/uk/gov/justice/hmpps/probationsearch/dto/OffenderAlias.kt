package uk.gov.justice.hmpps.probationsearch.dto

import java.time.LocalDate

data class OffenderAlias(
  val id: String? = null,
  val dateOfBirth: LocalDate? = null,
  val firstName: String? = null,
  val middleNames: List<String>? = null,
  val surname: String? = null,
  val gender: String? = null
)
