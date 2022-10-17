package uk.gov.justice.hmpps.offendersearch.cvlsearch

import java.time.LocalDate

data class LicenceCaseloadPerson(
  val name: Name,
  val identifiers: Identifiers,
  val manager: Manager,
  val allocationDate: LocalDate
)

data class Name(
  val surname: String,
  val forename: String,
  val middleName: String? = null
)

data class Identifiers(
  val crn: String,
  val cro: String?,
  val noms: String?,
  val pnc: String?
)

data class Manager(val code: String, val name: Name, val team: Team, val probationArea: CodedValue)

data class Team(
  val code: String,
  val description: String,
  val borough: CodedValue,
  val district: CodedValue
)

data class CodedValue(val code: String, val description: String)
