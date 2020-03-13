package uk.gov.justice.hmpps.offendersearch.dto

data class PhoneNumber(
    val number: String? = null,
    val type: PhoneTypes? = null
) {

  enum class PhoneTypes {
    TELEPHONE, MOBILE
  }
}