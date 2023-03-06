package uk.gov.justice.hmpps.probationsearch.dto

data class ContactDetails(
  val phoneNumbers: List<PhoneNumber>? = null,
  val emailAddresses: List<String>? = null,
  val allowSMS: Boolean? = null,
  val addresses: List<Address>? = null
)
