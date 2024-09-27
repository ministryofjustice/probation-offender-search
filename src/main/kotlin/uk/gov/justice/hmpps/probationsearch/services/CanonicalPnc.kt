package uk.gov.justice.hmpps.probationsearch.services

internal fun String.canonicalPNCNumber() =
  if (this.isPNCNumber()) {
    toPNCNumber()
  } else {
    this
  }

internal fun String.canonicalPNCNumberOrNull(): String? =
  if (this.isPNCNumber()) {
    toPNCNumber()
  } else {
    null
  }

private fun String.isPNCNumber(): Boolean {
  return this.matches("^([0-9]{2}|[0-9]{4})/[0-9]{1,7}[a-zA-Z]".toRegex())
}

private fun String.toPNCNumber(): String {
  val (year, serial) = this.split("/")
  val serialNumber = serial.substring(0, serial.length - 1).toInt()
  val checksum = this.last()

  return "$year/$serialNumber$checksum".lowercase()
}
