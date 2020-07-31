package uk.gov.justice.hmpps.offendersearch.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import io.restassured.RestAssured
import io.restassured.RestAssured.given
import io.restassured.config.ObjectMapperConfig
import io.restassured.config.RestAssuredConfig
import org.elasticsearch.client.RestHighLevelClient
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_CLASS
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestContext
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.support.AbstractTestExecutionListener
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener
import uk.gov.justice.hmpps.offendersearch.dto.MatchRequest
import uk.gov.justice.hmpps.offendersearch.dto.OffenderAlias
import uk.gov.justice.hmpps.offendersearch.dto.OffenderDetail
import uk.gov.justice.hmpps.offendersearch.util.JwtAuthenticationHelper
import uk.gov.justice.hmpps.offendersearch.util.LocalStackHelper
import java.lang.reflect.Type
import java.time.LocalDate
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["test", "localstack"])
@ExtendWith(SpringExtension::class)
@TestExecutionListeners(listeners = [DependencyInjectionTestExecutionListener::class, OffenderMatchControllerAPIIntegrationTest::class])
@ContextConfiguration
@DirtiesContext
abstract class OffenderMatchAPIIntegrationBase : AbstractTestExecutionListener() {
  @Autowired
  internal lateinit var jwtAuthenticationHelper: JwtAuthenticationHelper

  @Autowired
  @Qualifier("elasticSearchClient")
  private lateinit var esClient: RestHighLevelClient

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Value("\${search.supported.mapping.version}")
  private lateinit var mappingVersion: String

  override fun beforeTestClass(testContext: TestContext) {
    val objectMapper = testContext.applicationContext.getBean(ObjectMapper::class.java)
    val esClient = testContext.applicationContext.getBean(RestHighLevelClient::class.java)
    val mappingVersion = testContext.applicationContext.environment.getProperty("search.supported.mapping.version")
    LocalStackHelper(esClient, "v${mappingVersion}").loadData()
    RestAssured.port = Objects.requireNonNull(testContext.applicationContext.environment.getProperty("local.server.port"))!!.toInt()
    RestAssured.config = RestAssuredConfig.config().objectMapperConfig(
        ObjectMapperConfig().jackson2ObjectMapperFactory { _: Type?, _: String? -> objectMapper })
  }


  fun loadOffenders(vararg offenders: OffenderIdentification) {
    val template = "/elasticsearchdata/offender-template.json".readResourceAsText()
    val templateOffender = objectMapper.readValue(template, OffenderDetail::class.java)

    val offendersToLoad = offenders.map {
      templateOffender.copy(
          surname = it.surname,
          firstName = it.firstName,
          dateOfBirth = it.dateOfBirth,
          currentDisposal = if (it.activeSentence) "1" else "0",
          softDeleted = it.deleted,
          otherIds = templateOffender.otherIds?.copy(
              crn = it.crn,
              nomsNumber = it.nomsNumber,
              croNumber = it.croNumber,
              pncNumber = it.pncNumber
          ),
          offenderAliases = it.aliases.map { alias ->  OffenderAlias(
              firstName = alias.firstName,
              surname = alias.surname,
              dateOfBirth = alias.dateOfBirth
          ) }
      )
    }.map { objectMapper.writeValueAsString(it) }

    LocalStackHelper(esClient, "v${mappingVersion}").loadData(offendersToLoad)
  }

}

private fun String.readResourceAsText(): String {
  return OffenderMatchAPIIntegrationBase::class.java.getResource(this).readText()
}

data class OffenderIdentification(
    val surname: String,
    val firstName: String,
    val dateOfBirth: LocalDate,
    val crn: String,
    val activeSentence: Boolean = true,
    val deleted: Boolean = false,
    val aliases: List<Alias> = listOf(),
    val nomsNumber: String? = null,
    val croNumber: String? = null,
    val pncNumber: String? = null
)

data class Alias(
    val surname: String,
    val firstName: String,
    val dateOfBirth: LocalDate
)



