package uk.gov.justice.hmpps.offendersearch.dto

import io.swagger.annotations.ApiModelProperty
import java.time.LocalDate

data class Address(
  @ApiModelProperty(required = true) val from: LocalDate? = null,
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
