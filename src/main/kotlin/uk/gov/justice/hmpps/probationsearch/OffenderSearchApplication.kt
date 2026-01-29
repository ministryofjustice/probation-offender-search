package uk.gov.justice.hmpps.probationsearch

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.web.config.EnableSpringDataWebSupport

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
class OffenderSearchApplication

fun main(args: Array<String>) {
  runApplication<OffenderSearchApplication>(*args)
}
