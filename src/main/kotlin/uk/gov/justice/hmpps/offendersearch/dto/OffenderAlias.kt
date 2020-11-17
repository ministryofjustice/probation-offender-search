package uk.gov.justice.hmpps.offendersearch.dto

import java.time.LocalDate

data class OffenderAlias(
  val dateOfBirth: LocalDate? = null,
  val firstName: String? = null,
  val middleNames: List<String>? = null,
  val surname: String? = null,
  val gender: String? = null
)
