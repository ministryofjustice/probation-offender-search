package uk.gov.justice.hmpps.probationsearch.addresssearch

import com.fasterxml.jackson.annotation.JsonAlias
import java.time.LocalDate
import java.time.ZonedDateTime

data class AddressSearchRequest(
  val buildingName: String?,
  val addressNumber: String?,
  val streetName: String?,
  val district: String?,
  val town: String?,
  val county: String?,
  val postcode: String?,
  val telephoneNumber: String?,
  val boostOptions: BoostOptions = BoostOptions(),
)
data class BoostOptions(
  val postcode: Float = 20f,
  val streetName: Float = 10f,
  val buildingName: Float = 5f,
)

data class CodedValue(
  val code: String? = null,
  val description: String? = null,
)

data class Person(
  val id: Long,
  val crn: String,
  val dob: LocalDate,
  val gender: String,
)

data class Address(
  val id: Long,
  val buildingName: String? = null,
  val addressNumber: String? = null,
  val streetName: String? = null,
  val district: String? = null,
  val town: String? = null,
  val county: String? = null,
  val postcode: String? = null,
  val telephoneNumber: String? = null,
  val startDate: LocalDate,
  val endDate: LocalDate? = null,
  val notes: String? = null,
  val createdDateTime: ZonedDateTime,
  val lastUpdatedDateTime: ZonedDateTime,
  val status: CodedValue,
  val type: CodedValue? = null,
  val noFixedAbode: Boolean,
)
data class AddressSearchResponses(
  val personAddresses: List<AddressSearchResponse>,
)

data class AddressSearchResponse(
  val person: Person,
  val address: Address,
  val matchScore: Int,
)

data class PersonDetail(
  @JsonAlias("offenderId")
  val id: Long,
  @JsonAlias("otherIds")
  val identifiers: Identifiers,
  val dateOfBirth: LocalDate,
  val gender: String,
)

data class Identifiers(
  val crn: String,
)

data class PersonAddress(
  val id: Long,
  val from: LocalDate,
  val to: LocalDate? = null,
  val noFixedAbode: Boolean,
  val notes: String? = null,
  val addressNumber: String? = null,
  val buildingName: String? = null,
  val streetName: String? = null,
  val district: String? = null,
  val town: String? = null,
  val county: String? = null,
  val postcode: String? = null,
  val telephoneNumber: String? = null,
  val status: CodedValue,
  val type: CodedValue?,
  val createdDateTime: ZonedDateTime,
  val lastUpdatedDateTime: ZonedDateTime,
)

fun PersonDetail.toPerson() = Person(
  id,
  identifiers.crn,
  dateOfBirth,
  gender,
)

fun PersonAddress.toAddress() = Address(
  id,
  buildingName,
  addressNumber,
  streetName,
  district,
  town,
  county,
  postcode,
  telephoneNumber,
  from,
  to,
  notes,
  createdDateTime,
  lastUpdatedDateTime,
  status,
  type,
  noFixedAbode,
)
