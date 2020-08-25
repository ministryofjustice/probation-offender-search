package uk.gov.justice.hmpps.offendersearch.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.info.BuildProperties
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException

@Component
class HealthInfo(@param:Autowired(required = false) private val buildProperties: BuildProperties?) : HealthIndicator {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
  override fun health(): Health {
    return try { // TODO: Ping the remote Delius elasticSearch service when this is catered for
      Health.up().withDetail("version", version).build()
    } catch (e: RestClientException) {
      Health.down().withDetail("problem", e.message).build()
    }
  }

  private val version: String
    get() = if (buildProperties == null) "version not available" else buildProperties.version
}