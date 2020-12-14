package uk.gov.justice.hmpps.offendersearch.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import io.restassured.RestAssured
import org.elasticsearch.client.RestHighLevelClient
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.hmpps.offendersearch.dto.OffenderAlias
import uk.gov.justice.hmpps.offendersearch.dto.OffenderDetail
import uk.gov.justice.hmpps.offendersearch.dto.ProbationArea
import uk.gov.justice.hmpps.offendersearch.dto.Team
import uk.gov.justice.hmpps.offendersearch.util.JwtAuthenticationHelper
import uk.gov.justice.hmpps.offendersearch.util.LocalStackHelper
import kotlin.random.Random.Default.nextInt

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["test", "localstack"])
abstract class LocalstackIntegrationBase {

  @Autowired
  internal lateinit var jwtAuthenticationHelper: JwtAuthenticationHelper

  @Value("\${local.server.port}")
  private var port: Int = 0

  @BeforeEach
  internal fun before() {
    RestAssured.port = port
  }

  @Autowired
  @Qualifier("elasticSearchClient")
  private lateinit var esClient: RestHighLevelClient

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Value("\${search.supported.mapping.version}")
  private lateinit var mappingVersion: String
  fun loadOffenders(vararg offenders: OffenderReplacement) {
    val template = "/elasticsearchdata/offender-template.json".readResourceAsText()
    val templateOffender = objectMapper.readValue(template, OffenderDetail::class.java)

    val offendersToLoad = offenders.map {
      templateOffender.copy(
        offenderId = it.offenderId,
        surname = it.surname,
        firstName = it.firstName,
        middleNames = it.middleNames,
        dateOfBirth = it.dateOfBirth,
        softDeleted = it.deleted,
        gender = it.gender,
        otherIds = templateOffender.otherIds?.copy(
          crn = it.crn,
          nomsNumber = it.nomsNumber,
          croNumber = it.croNumber,
          pncNumber = it.pncNumber,
          niNumber = it.niNumber
        ),
        offenderAliases = it.aliases.map { alias ->
          OffenderAlias(
            id = nextInt().toString(),
            firstName = alias.firstName,
            surname = alias.surname,
            dateOfBirth = alias.dateOfBirth
          )
        },
        contactDetails = templateOffender.contactDetails?.copy(
          addresses = templateOffender.contactDetails?.addresses?.map { address ->
            if (address.status?.code == "M") address.copy(
              streetName = it.streetName,
              town = it.town,
              county = it.county,
              postcode = it.postcode
            ) else address
          }
        ),
        offenderManagers = templateOffender.offenderManagers?.map { offenderManager ->
          it.offenderManagers.find { replacement -> replacement.active == offenderManager.active }
            .let { matchingReplacement ->
              offenderManager.copy(
                probationArea = ProbationArea(code = matchingReplacement?.code, description = matchingReplacement?.description),
                team = Team(code = matchingReplacement?.team?.code, localDeliveryUnit = matchingReplacement?.team?.localDeliveryUnit),
                softDeleted = matchingReplacement?.softDeleted
              )
            }
        },
        currentExclusion = it.currentExclusion,
        currentRestriction = it.currentRestriction
      )
    }.map { objectMapper.writeValueAsString(it) }

    LocalStackHelper(esClient, "v$mappingVersion").loadData(offendersToLoad)
  }

  private fun String.readResourceAsText(): String {
    return LocalstackIntegrationBase::class.java.getResource(this).readText()
  }
}

fun Int.toCrn() = "X%05d".format(this)

fun Int.toNomsNumber() = "G%d0GG".format(this)
