@file:Suppress("DEPRECATION")

package uk.gov.justice.hmpps.offendersearch.config

import org.apache.commons.codec.binary.Base64
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext
import org.springframework.security.oauth2.client.OAuth2ClientContext
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer
import org.springframework.security.oauth2.provider.token.DefaultTokenServices
import org.springframework.security.oauth2.provider.token.TokenStore
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.ApiInfo
import springfox.documentation.service.Contact
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2
import uk.gov.justice.hmpps.offendersearch.controllers.OffenderSearchController
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*

@Configuration
@EnableSwagger2
@EnableGlobalMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
class ResourceServerConfiguration : ResourceServerConfigurerAdapter() {
  @Value("\${jwt.public.key}")
  private val jwtPublicKey: String? = null
  @Autowired(required = false)
  private val buildProperties: BuildProperties? = null

  override fun configure(http: HttpSecurity) {
    http
        .sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Can't have CSRF protection as requires session
        .and().csrf().disable()
        .authorizeRequests()
        .antMatchers(
            "/webjars/**",
            "/favicon.ico",
            "/csrf",
            "/health/**",
            "/info",
            "/v2/api-docs",
            "/swagger-ui.html",
            "/swagger-resources",
            "/swagger-resources/configuration/ui",
            "/swagger-resources/configuration/security").permitAll()
        .anyRequest()
        .authenticated()
  }

  override fun configure(config: ResourceServerSecurityConfigurer) {
    config.tokenServices(tokenServices())
  }

  @Bean
  fun tokenStore(): TokenStore {
    return JwtTokenStore(accessTokenConverter())
  }

  @Bean
  fun accessTokenConverter(): JwtAccessTokenConverter {
    val converter = JwtAccessTokenConverter()
    converter.setVerifierKey(String(Base64.decodeBase64(jwtPublicKey)))
    return converter
  }

  @Bean
  @Primary
  fun tokenServices(): DefaultTokenServices {
    val defaultTokenServices = DefaultTokenServices()
    defaultTokenServices.setTokenStore(tokenStore())
    return defaultTokenServices
  }

  @Bean
  fun api(): Docket {
    val apiInfo = ApiInfo(
        "Offender search API Documentation",
        "API for searching for offenders in Delius.",
        version, "", contactInfo(), "", "", emptyList())
    val docket = Docket(DocumentationType.SWAGGER_2)
        .useDefaultResponseMessages(false)
        .apiInfo(apiInfo)
        .select()
        .apis(RequestHandlerSelectors.basePackage(OffenderSearchController::class.java.getPackage().name))
        .paths(PathSelectors.any())
        .build()
    docket.genericModelSubstitutes(Optional::class.java)
    docket.directModelSubstitute(ZonedDateTime::class.java, Date::class.java)
    docket.directModelSubstitute(LocalDateTime::class.java, Date::class.java)
    return docket
  }

  /**
   * @return health data. Note this is unsecured so no sensitive data allowed!
   */
  private val version: String
    get() = if (buildProperties == null) "version not available" else buildProperties.version

  private fun contactInfo(): Contact {
    return Contact(
        "HMPPS Digital Studio",
        "",
        "feedback@digital.justice.gov.uk")
  }

  @Bean
  fun oAuth2ClientContext(): OAuth2ClientContext {
    return DefaultOAuth2ClientContext()
  }
}