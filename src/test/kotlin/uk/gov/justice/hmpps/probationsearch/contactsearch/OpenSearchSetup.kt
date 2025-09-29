package uk.gov.justice.hmpps.probationsearch.contactsearch

import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.withPollInterval
import org.opensearch.client.Request
import org.opensearch.client.RestClient
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.Refresh
import org.opensearch.client.opensearch._types.VersionType
import org.opensearch.client.opensearch.core.bulk.BulkOperation
import org.opensearch.client.opensearch.ml.ModelFormat
import org.opensearch.data.client.orhlc.OpenSearchRestTemplate
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.data.elasticsearch.core.query.Query
import org.springframework.stereotype.Component
import org.springframework.util.ResourceUtils
import uk.gov.justice.hmpps.probationsearch.contactsearch.semantic.ContactSemanticSearchService.Companion.INDEX_NAME
import java.time.Duration.ofSeconds
import java.util.*

@Component
class OpenSearchSetup {
  companion object {
    fun file(path: String) = ResourceUtils.getFile(path).readText()
    private const val SEMANTIC_PRIMARY = "contact-semantic-search-primary"
    private val INDEX_TEMPLATE_JSON = file("classpath:search-setup/contact-semantic-index-template.json")
    private val BLOCK_TEMPLATE_JSON = file("classpath:search-setup/contact-semantic-block-template.json")
    private val BLOCK_PIPELINE_JSON = file("classpath:search-setup/contact-semantic-block-pipeline.json")
    private val INGEST_PIPELINE_JSON = file("classpath:search-setup/contact-semantic-ingest-pipeline.tpl.json")
    private val SEARCH_PIPELINE_JSON = file("classpath:search-setup/contact-semantic-search-pipeline.tpl.json")

    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Autowired
  private lateinit var restClient: RestClient

  @Autowired
  internal lateinit var openSearchRestTemplate: OpenSearchRestTemplate

  @Autowired
  internal lateinit var openSearchClient: OpenSearchClient

  fun setup() {
    if (
      openSearchClient.indices().exists { it.index(SEMANTIC_PRIMARY) }.value() &&
      openSearchClient.count { it.index(SEMANTIC_PRIMARY) }.count() == ContactGenerator.contacts.size.toLong()
    ) {
      log.debug("OpenSearch has already been seeded. Delete/recreate the container to re-seed it.")
      return
    }

    log.info("Creating templates...")
    restClient.performRequest(
      Request("PUT", "/_index_template/contact-semantic-index-template").apply {
        setJsonEntity(INDEX_TEMPLATE_JSON)
      },
    )
    restClient.performRequest(
      Request("PUT", "/_index_template/contact-semantic-block-template").apply {
        setJsonEntity(BLOCK_TEMPLATE_JSON)
      },
    )

    log.info("Creating indices...")
    createIndex("contact-semantic-search-a", "contact-semantic-search-primary")
    createIndex("contact-semantic-block-a", "contact-semantic-block-primary")

    log.info("Creating model...")
    val modelId = with(openSearchClient.ml()) {
      val modelGroupId = registerModelGroup { it.name(UUID.randomUUID().toString()) }.modelGroupId()
      val modelId = retry {
        val registerTaskId = registerModel {
          // see https://docs.opensearch.org/latest/ml-commons-plugin/pretrained-models/
          it.name("huggingface/sentence-transformers/msmarco-distilbert-base-tas-b")
            .version("1.0.3")
            .modelFormat(ModelFormat.Onnx)
            .modelGroupId(modelGroupId)
        }.taskId()

        await atMost ofSeconds(60) withPollInterval ofSeconds(1) untilCallTo {
          getTask { it.taskId(registerTaskId) }.state()
        } matches { it == "COMPLETED" }

        getTask { it.taskId(registerTaskId) }.modelId()!!
      }

      retry {
        val deployTaskId = deployModel { it.modelId(modelId) }.taskId()

        await atMost ofSeconds(60) untilCallTo {
          getTask { it.taskId(deployTaskId) }.state()
        } matches { it == "COMPLETED" }
      }
      modelId
    }

    log.info("Creating pipelines...")
    restClient.performRequest(
      Request("PUT", "/_ingest/pipeline/contact-semantic-search-pipeline").apply {
        setJsonEntity(INGEST_PIPELINE_JSON.replace($$"${model_id}", modelId))
      },
    )
    restClient.performRequest(
      Request("PUT", "/_search/pipeline/contact-semantic-search-search-pipeline").apply {
        setJsonEntity(SEARCH_PIPELINE_JSON.replace($$"${model_id}", modelId))
      },
    )
    restClient.performRequest(
      Request("PUT", "/_ingest/pipeline/contact-semantic-block-pipeline").apply {
        setJsonEntity(BLOCK_PIPELINE_JSON)
      },
    )

    log.info("Loading data...")
    val operations = ContactGenerator.contacts.map { contact ->
      BulkOperation.of { bulk ->
        bulk.index {
          it.id(contact.id.toString())
            .version(1).versionType(VersionType.External)
            .document(contact)
        }
      }
    }
    val response = openSearchClient.bulk { it.index(INDEX_NAME).refresh(Refresh.True).operations(operations) }
    val errors = response.items().map { it.error() }
      .filter { it != null && it.type() != "version_conflict_engine_exception" }
    if (errors.isNotEmpty())
      error("${errors.size} indexing errors found. First with reason: ${errors.first()?.reason()}")

    await untilCallTo {
      openSearchRestTemplate.count(Query.findAll(), IndexCoordinates.of(SEMANTIC_PRIMARY))
    } matches { it == ContactGenerator.contacts.size.toLong() }
  }

  private fun <T> retry(maxAttempts: Int = 3, function: () -> T): T {
    repeat(maxAttempts) {
      try {
        return function.invoke()
      } catch (e: Exception) {
        log.warn("Attempt #$it failed with exception: ${e.message}")
        if (it == maxAttempts - 1) throw e
      }
    }
    error("Max attempts reached")
  }

  private fun createIndex(indexName: String, aliasName: String) {
    with(openSearchClient.indices()) {
      if (exists { it.index(indexName) }.value()) {
        delete { it.index(indexName) }
      }
      create { it.index(indexName).aliases(aliasName, { alias -> alias }) }
    }
  }

}