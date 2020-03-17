package uk.gov.justice.hmpps.offendersearch.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.ApiInfo
import springfox.documentation.service.Contact
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import uk.gov.justice.hmpps.offendersearch.controllers.OffenderSearchController
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*

@Configuration
class DocketConfiguration {
  @Autowired(required = false)
  private val buildProperties: BuildProperties? = null


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

}