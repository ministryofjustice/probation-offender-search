package uk.gov.justice.hmpps.probationsearch.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.License
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.annotations.servers.Server
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders

@Configuration
@OpenAPIDefinition(
  info = io.swagger.v3.oas.annotations.info.Info(
    title = "Offender search API Documentation",
    contact = io.swagger.v3.oas.annotations.info.Contact(
      name = "Probation Integration Team",
      email = "probation-integration-team@digital.justice.gov.uk",
      url = "https://mojdt.slack.com/archives/C02HQ4M2YQN" // #probation-integration-tech Slack channel
    ),
    version = "1.0"
  ),
  servers = [Server(url = "/")],
  security = [SecurityRequirement(name = "hmpps-auth-token")]
)
@SecurityScheme(
  name = "hmpps-auth-token",
  scheme = "bearer",
  bearerFormat = "JWT",
  type = SecuritySchemeType.HTTP,
  `in` = SecuritySchemeIn.HEADER,
  paramName = HttpHeaders.AUTHORIZATION
)
class OpenAPIConfiguration
