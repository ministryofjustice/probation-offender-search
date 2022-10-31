package uk.gov.justice.hmpps.offendersearch.dto

import java.time.LocalDate

data class Address(
  val id: Long,
  val from: LocalDate,
  val to: LocalDate? = null,
  val noFixedAbode: Boolean? = null,
  val notes: String? = null,
  val addressNumber: String? = null,
  val buildingName: String? = null,
  val streetName: String? = null,
  val district: String? = null,
  val town: String? = null,
  val county: String? = null,
  val postcode: String? = null,
  val telephoneNumber: String? = null,
  val status: KeyValue? = null
)
