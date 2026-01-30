package uk.gov.justice.hmpps.probationsearch.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.boot.info.BuildProperties
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException

@Component
class HealthInfo(@param:Autowired private val buildProperties: BuildProperties) : HealthIndicator {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  override fun health(): Health {
    return try {
      Health.up().withDetail("version", version).build()
    } catch (e: RestClientException) {
      Health.down().withDetail("problem", e.message.toString()).build()
    }
  }

  private val version: String
    get() = buildProperties.version.toString()
}
