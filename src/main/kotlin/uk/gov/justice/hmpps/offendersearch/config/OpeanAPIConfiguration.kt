package uk.gov.justice.hmpps.offendersearch.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpeanAPIConfiguration {
  @Autowired(required = false)
  private val buildProperties: BuildProperties? = null

  @Bean
  fun api(): OpenAPI {
    return OpenAPI()
      .info(
        Info().title("Offender search API Documentation")
          .description("API for searching for offenders in Delius.")
          .version(version)
          .contact(contactInfo())
      )
  }

  /**
   * @return health data. Note this is unsecured so no sensitive data allowed!
   */
  private val version: String
    get() = if (buildProperties == null) "version not available" else buildProperties.version

  private fun contactInfo(): Contact {
    return Contact().name("HMPPS Digital Studio").email("feedback@digital.justice.gov.uk")
  }
}
