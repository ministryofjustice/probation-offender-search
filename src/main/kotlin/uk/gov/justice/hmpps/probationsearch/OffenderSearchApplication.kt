package uk.gov.justice.hmpps.probationsearch

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(
  exclude = [ElasticsearchDataAutoConfiguration::class],
  scanBasePackages = ["uk.gov.justice.digital.hmpps", "uk.gov.justice.hmpps"],
)
class OffenderSearchApplication

fun main(args: Array<String>) {
  runApplication<OffenderSearchApplication>(*args)
}
