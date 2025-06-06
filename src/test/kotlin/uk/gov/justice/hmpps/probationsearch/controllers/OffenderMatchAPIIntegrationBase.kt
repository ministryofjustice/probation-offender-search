package uk.gov.justice.hmpps.probationsearch.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import io.restassured.RestAssured
import org.junit.jupiter.api.BeforeEach
import org.opensearch.client.RestHighLevelClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.justice.hmpps.probationsearch.dto.OffenderAlias
import uk.gov.justice.hmpps.probationsearch.dto.OffenderDetail
import uk.gov.justice.hmpps.probationsearch.services.FeatureFlags
import uk.gov.justice.hmpps.probationsearch.util.JwtAuthenticationHelper
import uk.gov.justice.hmpps.probationsearch.util.PersonSearchHelper
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@MockitoBean(types = [FeatureFlags::class])
@ActiveProfiles(profiles = ["test"])
@DirtiesContext
abstract class OffenderMatchAPIIntegrationBase {
  @Autowired
  internal lateinit var jwtAuthenticationHelper: JwtAuthenticationHelper

  @Autowired
  @Qualifier("openSearchClient")
  private lateinit var openSearchClient: RestHighLevelClient

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Value("\${local.server.port}")
  private var port: Int = 0

  @BeforeEach
  internal fun before() {
    RestAssured.port = port
  }

  fun loadOffenders(vararg offenders: OffenderIdentification) {
    val template = "/search-data/offender-template.json".readResourceAsText()
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
          pncNumber = it.pncNumber,
        ),
        offenderAliases = it.aliases.map { alias ->
          OffenderAlias(
            firstName = alias.firstName,
            surname = alias.surname,
            dateOfBirth = alias.dateOfBirth,
          )
        },
      )
    }.map { objectMapper.writeValueAsString(it) }

    PersonSearchHelper(openSearchClient).loadData(offendersToLoad)
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
  val pncNumber: String? = null,
)

data class Alias(
  val surname: String,
  val firstName: String,
  val dateOfBirth: LocalDate,
)
