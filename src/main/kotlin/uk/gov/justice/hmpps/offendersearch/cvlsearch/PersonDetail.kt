package uk.gov.justice.hmpps.offendersearch.cvlsearch

import com.fasterxml.jackson.annotation.JsonAlias
import java.time.LocalDate

data class PersonDetail(
  @JsonAlias("otherIds")
  val identifiers: PersonIdentifiers,
  val firstName: String,
  val middleNames: List<String>,
  val surname: String,
  val offenderManagers: List<PersonManager>
)

data class PersonIdentifiers(
  val crn: String,
  val croNumber: String?,
  val nomsNumber: String?,
  val pncNumber: String?
)

data class PersonManager(
  val staff: Staff,
  val team: Team,
  val probationArea: CodedValue,
  val fromDate: LocalDate
)

data class Staff(
  val code: String,
  val forenames: String,
  val surname: String,
)
