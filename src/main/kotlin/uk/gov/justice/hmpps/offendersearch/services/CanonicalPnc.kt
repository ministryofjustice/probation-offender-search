package uk.gov.justice.hmpps.offendersearch.services

internal fun String.canonicalPNCNumber(): String {
  return if (this.isPNCNumber()) {
    val (year, serial) = this.split("/")
    val serialNumber =  serial.substring(0, serial.length - 1).toInt()
    val checksum = this.last()

    "$year/$serialNumber$checksum".toLowerCase()
  } else this

}


private fun String.isPNCNumber(): Boolean {
  return this.matches("^([0-9]{2}|[0-9]{4})/[0-9]+[a-zA-Z]".toRegex())
}