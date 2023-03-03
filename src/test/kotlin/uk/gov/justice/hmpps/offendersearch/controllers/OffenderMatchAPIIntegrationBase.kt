package uk.gov.justice.hmpps.offendersearch.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import io.restassured.RestAssured
import org.elasticsearch.client.RestHighLevelClient
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.hmpps.offendersearch.dto.OffenderAlias
import uk.gov.justice.hmpps.offendersearch.dto.OffenderDetail
import uk.gov.justice.hmpps.offendersearch.util.ElasticsearchHelper
import uk.gov.justice.hmpps.offendersearch.util.JwtAuthenticationHelper
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["test", "localstack"])
@DirtiesContext
abstract class OffenderMatchAPIIntegrationBase {
  @Autowired
  internal lateinit var jwtAuthenticationHelper: JwtAuthenticationHelper

  @Autowired
  @Qualifier("elasticSearchClient")
  private lateinit var esClient: RestHighLevelClient

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Value("\${search.supported.mapping.version}")
  private lateinit var mappingVersion: String

  @Value("\${local.server.port}")
  private var port: Int = 0

  @BeforeEach
  internal fun before() {
    RestAssured.port = port
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
        otherIds = templateOffender.otherIds.copy(
          crn = it.crn,
          nomsNumber = it.nomsNumber,
          croNumber = it.croNumber,
          pncNumber = it.pncNumber
        ),
        offenderAliases = it.aliases.map { alias ->
          OffenderAlias(
            firstName = alias.firstName,
            surname = alias.surname,
            dateOfBirth = alias.dateOfBirth
          )
        }
      )
    }.map { objectMapper.writeValueAsString(it) }

    ElasticsearchHelper(esClient).loadData(offendersToLoad)
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
