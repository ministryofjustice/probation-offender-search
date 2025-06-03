package uk.gov.justice.hmpps.probationsearch.contactsearch

import io.restassured.RestAssured
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.opensearch.action.admin.indices.delete.DeleteIndexRequest
import org.opensearch.action.ingest.PutPipelineRequest
import org.opensearch.client.RequestOptions
import org.opensearch.client.indices.CreateIndexRequest
import org.opensearch.client.indices.GetIndexRequest
import org.opensearch.client.indices.PutIndexTemplateRequest
import org.opensearch.common.xcontent.XContentType.JSON
import org.opensearch.core.common.bytes.BytesArray
import org.opensearch.core.xcontent.MediaType
import org.opensearch.data.client.orhlc.OpenSearchRestTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.data.elasticsearch.core.query.IndexQuery
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.util.ResourceUtils
import uk.gov.justice.hmpps.probationsearch.IndexNotReadyException
import uk.gov.justice.hmpps.probationsearch.contactsearch.ContactBlockService.Companion.CONTACT_SEMANTIC_BLOCK
import uk.gov.justice.hmpps.probationsearch.services.FeatureFlags
import uk.gov.justice.hmpps.probationsearch.util.JwtAuthenticationHelper
import uk.gov.justice.hmpps.probationsearch.wiremock.DeliusApiExtension
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@ExtendWith(DeliusApiExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@MockitoBean(types = [FeatureFlags::class])
@ActiveProfiles(profiles = ["test"])
class ContactSemanticBlockIntegrationTest {

  @Autowired
  internal lateinit var jwtAuthenticationHelper: JwtAuthenticationHelper

  @Autowired
  internal lateinit var contactBlockService: ContactBlockService

  @Autowired
  internal lateinit var openSearchRestTemplate: OpenSearchRestTemplate

  @Value("\${local.server.port}")
  internal val port: Int = 0

  @BeforeEach
  internal fun before() {
    RestAssured.port = port
    val indexName = CONTACT_SEMANTIC_BLOCK

    openSearchRestTemplate.execute {
      it.ingest().putPipeline(
        PutPipelineRequest(
          "contact-semantic-block-pipeline",
          "/searchdata/contact-semantic-block-pipeline.json".resourceAsByteReference(),
          JSON
        ),
        RequestOptions.DEFAULT
      )
    }

    openSearchRestTemplate.execute {
      it.indices().putTemplate(
        PutIndexTemplateRequest("contact-semantic-block-template").source(
          TEMPLATE_JSON,
          MediaType.fromMediaType(JSON.mediaType()),
        ),
        RequestOptions.DEFAULT,
      )
    }
    openSearchRestTemplate.execute {
      if (it.indices().exists(GetIndexRequest(indexName), RequestOptions.DEFAULT)) {
        it.indices().delete(DeleteIndexRequest(indexName), RequestOptions.DEFAULT)
      }
      it.indices().create(CreateIndexRequest(indexName), RequestOptions.DEFAULT)
    }
  }

  private fun createBlock(crn: String, timestamp: String? = null) {
    val indexQuery = IndexQuery()
    indexQuery.id = crn
    indexQuery.`object` = BlockJson(crn, timestamp)
    openSearchRestTemplate.index(indexQuery, IndexCoordinates.of(CONTACT_SEMANTIC_BLOCK))
  }

  private fun blockExists(crn: String): Boolean {
    return openSearchRestTemplate.exists(crn, IndexCoordinates.of(CONTACT_SEMANTIC_BLOCK))
  }

  @Test
  fun `can block a crn, rollback and unblock after exception has occurred `() {
    val crn = "X123456"
    var rollbackActioned = false
    var numberOfTries = 0
    val ex = assertThrows<RuntimeException> {
      contactBlockService.doWithBlock(crn, 2) {
        action {
          numberOfTries++
          throw RuntimeException("Some exception")
        }
        rollback { rollbackActioned = true }
      }
    }
    assertThat(numberOfTries).isEqualTo(3)
    assertThat(ex.message).isEqualTo("Some exception")
    assertThat(rollbackActioned).isTrue()
  }

  @Test
  fun `can block a crn and unblock after first successful run `() {
    val crn = "X123456"
    var rollbackActioned = false
    var actionsPerformed = 0
    assertDoesNotThrow {
      contactBlockService.doWithBlock(crn, 2) {
        action { actionsPerformed++ }
        rollback { rollbackActioned = true }
      }
    }
    assertThat(actionsPerformed).isEqualTo(1)
    assertThat(rollbackActioned).isFalse()
  }

  @Test
  fun `stale block exists that is longer ago than 5 minutes - rollback is called and block is removed`() {
    val crn = "X654321"
    var rollbackActioned = false
    createBlock(
      crn,
      DateTimeFormatter.ofPattern(ContactBlockService.CONTACT_SEMANTIC_BLOCK_TIMESTAMP).format(
        LocalDateTime.now(ZoneId.of("UTC")).minusMinutes(6)
      )
    )
    assertDoesNotThrow {
      contactBlockService.checkIfBlockedOrRollbackIfStale(crn) { rollbackActioned = true }
    }
    assertThat(rollbackActioned).isTrue()
    assertThat(blockExists(crn)).isFalse()
  }

  @Test
  fun `block exists that is not longer ago than 5 minutes - rollback is not called and block is not removed - Index not ready exception is thrown after 2 attempts`() {
    val crn = "X000001"
    var rollbackActioned = false
    createBlock(
      crn,
      DateTimeFormatter.ofPattern(ContactBlockService.CONTACT_SEMANTIC_BLOCK_TIMESTAMP).format(
        LocalDateTime.now(ZoneId.of("UTC")).minusMinutes(4)
      )
    )
    val ex = assertThrows<IndexNotReadyException> {
      contactBlockService.checkIfBlockedOrRollbackIfStale(crn, retries = 1) {
        rollback { rollbackActioned = true }
      }
    }
    assertThat(ex.message).isEqualTo("Index for $crn is still blocked")
    assertThat(rollbackActioned).isFalse()
    assertThat(blockExists(crn)).isTrue()
  }

  companion object {
    private val TEMPLATE_JSON =
      ResourceUtils.getFile("classpath:searchdata/contact-semantic-block-template.json").readText()
  }
}

private fun String.resourceAsByteReference() =
  BytesArray(ContactSemanticBlockIntegrationTest::class.java.getResource(this)!!.readBytes())
