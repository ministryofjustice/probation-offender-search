package uk.gov.justice.hmpps.probationsearch.dto

import java.time.LocalDate

data class ProbationStatus(
  val status: String,
  val previouslyKnownTerminationDate: LocalDate? = null,
  val inBreach: Boolean? = null,
  val preSentenceActivity: Boolean = false,
  val awaitingPsr: Boolean = false,
)
