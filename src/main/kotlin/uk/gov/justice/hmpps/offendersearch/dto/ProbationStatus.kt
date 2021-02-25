package uk.gov.justice.hmpps.offendersearch.dto

import java.time.LocalDate

data class ProbationStatus(
  val status: String,
  val previouslyKnownTerminationDate: LocalDate? = null,
  val inBreach: Boolean? = null,
  val preSentenceActivity: Boolean = false
)
