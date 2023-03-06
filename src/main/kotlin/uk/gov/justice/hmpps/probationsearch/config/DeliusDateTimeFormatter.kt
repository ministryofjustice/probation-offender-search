package uk.gov.justice.hmpps.probationsearch.config

import java.time.ZoneId
import java.time.format.DateTimeFormatter

val DeliusDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")

val EuropeLondon: ZoneId = ZoneId.of("Europe/London")
