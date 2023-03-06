package uk.gov.justice.hmpps.probationsearch.cvlsearch

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern

private const val CRN_INPUT = "identifiers.crn"
private const val FORENAME_INPUT = "name.forename"
private const val SURNAME_INPUT = "name.surname"
private const val MANAGER_FORENAMES_INPUT = "manager.name.forename"
private const val MANAGER_SURNAME_INPUT = "manager.name.surname"
private const val FIELD_REGEX =
  "^identifiers\\.crn\$|^name\\.forename\$|^name\\.surname\$|^manager\\.name\\.forename\$|^manager\\.name\\.surname\$"
const val FIELD_MESSAGE =
  "should be one of '$CRN_INPUT', '$FORENAME_INPUT', '$SURNAME_INPUT', '$MANAGER_FORENAMES_INPUT' or '$MANAGER_SURNAME_INPUT'"
const val DIRECTION_MESSAGE = "should be one of 'asc' or 'desc'"

data class LicenceCaseloadRequest(
  @field:NotEmpty val teamCodes: List<String>,
  val query: String = "",
  @field:Valid val sortBy: List<SortBy> = listOf(SortBy("name.forename"), SortBy("name.surname")),
  val pageSize: Int = 100,
  val offset: Int = 0,
)

data class SortBy(
  @field:NotBlank
  @field:Pattern(
    regexp = FIELD_REGEX,
    message = FIELD_MESSAGE
  )
  val field: String,
  @field:Pattern(regexp = "^asc\$|^desc\$", message = DIRECTION_MESSAGE) val direction: String = "asc"
)

enum class SortField(val input: String, val searchField: String) {
  CRN(CRN_INPUT, "otherIds.crn.raw"),
  FORENAME(FORENAME_INPUT, "firstName.raw"),
  SURNAME(SURNAME_INPUT, "surname.raw"),
  MANAGER_FORENAME(MANAGER_FORENAMES_INPUT, "offenderManagers.staff.forenames.raw"),
  MANAGER_SURNAME(MANAGER_SURNAME_INPUT, "offenderManagers.staff.surname.raw");

  companion object {
    fun fromInput(input: String) = values().firstOrNull { it.input == input }
  }
}
