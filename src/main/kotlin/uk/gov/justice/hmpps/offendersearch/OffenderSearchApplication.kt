package uk.gov.justice.hmpps.offendersearch

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class OffenderSearchApplication

fun main(args: Array<String>) {
  runApplication<OffenderSearchApplication>(*args)
}
