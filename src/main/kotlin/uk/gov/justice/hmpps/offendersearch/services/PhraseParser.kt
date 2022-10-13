package uk.gov.justice.hmpps.offendersearch.services

import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun extractDateLikeTerms(phrase: String): List<String> {
  return phrase
    .split(" ")
    .filterNot(String::isEmpty)
    .mapNotNull(String::convertToDateOrNull)
    .map { it.format(DateTimeFormatter.ISO_DATE) }
}

fun extractPNCNumberLikeTerms(phrase: String): List<String> {
  return phrase
    .split(" ")
    .filterNot(String::isEmpty)
    .mapNotNull(String::canonicalPNCNumberOrNull)
}

fun extractCRONumberLikeTerms(phrase: String): List<String> {
  return phrase
    .split(" ")
    .filterNot(String::isEmpty)
    .mapNotNull(String::canonicalCRONumberOrNull)
}

fun extractPhoneNumberLikeTerms(phrase: String): List<String> {
  return phrase
    .split(" ")
    .filterNot(String::isEmpty)
    .mapNotNull(String::phoneNumberOrNull)
}

fun extractSearchableSimpleTerms(phrase: String): String {
  return phrase
    .split(" ")
    .asSequence()
    .filterNot(String::isEmpty)
    .filterNot(String::isDate)
    .filterNot { it.length == 1 }
    .filterNot { it.contains("/".toRegex()) }
    .map(String::toLowerCase)
    .joinToString(" ")
}

fun extractSearchableSimpleTermsWithSingleLetters(phrase: String): List<String> {
  return phrase
    .split(" ")
    .asSequence()
    .filterNot(String::isEmpty)
    .filterNot(String::isDate)
    .filterNot { it.contains("/".toRegex()) }
    .map(String::toLowerCase)
    .toList()
}

private fun String.convertToDateOrNull(): LocalDate? {
  return supportedDateFormatters
    .asSequence()
    .tryOrRemove { LocalDate.parse(this, it) }
    .firstOrNull()
}

private fun String.isDate(): Boolean = this.convertToDateOrNull()?.let { true } ?: false

private val supportedDateFormats: List<String> = listOf(
  "dd/MM/yyyy",
  "yyyy/MMMM/dd", "yyyy-MMMM-dd",
  "yyyy/MMMM/d", "yyyy-MMMM-d",
  "yyyy/MMM/dd", "yyyy-MMM-dd",
  "yyyy/MMM/d", "yyyy-MMM-d",
  "yyyy/MM/dd", "yyyy-MM-dd",
  "yyyy/M/dd", "yyyy-M-dd",
  "yyyy/MM/d", "yyyy-MM-d",
  "yyyy/M/d", "yyyy-M-d",
  "dd/MMMM/yyyy", "dd-MMMM-yyyy",
  "d/MMMM/yyyy", "d-MMMM-yyyy",
  "dd/MMM/yyyy", "dd-MMM-yyyy",
  "d/MMM/yyyy", "d-MMM-yyyy",
  "dd-MM-yyyy",
  "dd/M/yyyy", "dd-M-yyyy",
  "d/MM/yyyy", "d-MM-yyyy",
  "d/M/yyyy", "d-M-yyyy"
)

private val supportedDateFormatters by lazy { supportedDateFormats.map { DateTimeFormatter.ofPattern(it) } }

private fun <T, U : Any> Sequence<T>.tryOrRemove(block: (T) -> U): Sequence<U> {
  return mapNotNull {
    try {
      block(it)
    } catch (ex: Throwable) {
      null
    }
  }
}

private fun String.canonicalCRONumberOrNull(): String? =
  if (this.isCRONumber()) {
    this.toLowerCase()
  } else null

private fun String.isCRONumber(): Boolean {
  return this.matches("^[0-9]{1,6}/[0-9]{2}[a-zA-Z]".toRegex()) ||
    this.matches("^(sf|SF)[0-9]{2}/[0-9]{1,6}[a-zA-Z]".toRegex())
}

private fun String.phoneNumberOrNull(): String? =
  if (this.matches("^/d{6,10}$".toRegex())) {
    this
  } else null

