package uk.gov.justice.hmpps.probationsearch.security

import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity

@Configuration
@EnableMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
class MethodSecurityConfiguration
