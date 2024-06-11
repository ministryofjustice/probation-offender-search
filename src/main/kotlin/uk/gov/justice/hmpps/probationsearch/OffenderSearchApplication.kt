package uk.gov.justice.hmpps.probationsearch

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.data.web.config.EnableSpringDataWebSupport

@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@SpringBootApplication(exclude = [ElasticsearchDataAutoConfiguration::class])
class OffenderSearchApplication

fun main(args: Array<String>) {
  runApplication<OffenderSearchApplication>(*args)
}
