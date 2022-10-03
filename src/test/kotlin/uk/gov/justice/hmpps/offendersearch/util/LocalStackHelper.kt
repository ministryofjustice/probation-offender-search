package uk.gov.justice.hmpps.offendersearch.util

import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.ingest.PutPipelineRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.client.core.CountRequest
import org.elasticsearch.client.indices.CreateIndexRequest
import org.elasticsearch.client.indices.GetIndexRequest
import org.elasticsearch.client.indices.PutIndexTemplateRequest
import org.elasticsearch.common.bytes.BytesArray
import org.elasticsearch.common.xcontent.XContentType.JSON
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

class LocalStackHelper(private val esClient: RestHighLevelClient) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
    const val indexName = "person-search-primary"
    const val templateName = "person-search-template"
  }

  fun loadData() {
    rebuildIndex()
    loadOffender("1", "/elasticsearchdata/john-smith.json".resourceAsString())
    loadOffender("2", "/elasticsearchdata/jane-smith.json".resourceAsString())
    loadOffender("3", "/elasticsearchdata/sam-jones-deleted.json".resourceAsString())
    loadOffender("4", "/elasticsearchdata/antonio-gramsci-n01.json".resourceAsString())
    loadOffender("5", "/elasticsearchdata/antonio-gramsci-n02.json".resourceAsString())
    loadOffender("6", "/elasticsearchdata/antonio-gramsci-n03.json".resourceAsString())
    loadOffender("7", "/elasticsearchdata/anne-gramsci-n02.json".resourceAsString())
    loadOffender("8", "/elasticsearchdata/antonio-gramsci-c20.json".resourceAsString())
    loadOffender("9", "/elasticsearchdata/tom-bloggs.json".resourceAsString())
    waitForOffenderLoading(9)
  }

  fun loadData(offenders: List<String>) {
    rebuildIndex()

    offenders.forEach {
      loadOffender(UUID.randomUUID().toString(), it)
    }

    waitForOffenderLoading(offenders.size)
  }

  private fun rebuildIndex() {
    destroyIndex()
    buildPipeline()
    buildIndex()
  }

  private fun waitForOffenderLoading(expectedCount: Int) {
    await untilCallTo {
      esClient.count(CountRequest(indexName), RequestOptions.DEFAULT).count
    } matches { it == expectedCount.toLong() }
  }

  private fun loadOffender(key: String, offender: String) {
    log.debug("Loading offender: {}", offender)
    esClient.index(
      IndexRequest()
        .source(offender, JSON)
        .id(key)
        .type("_doc")
        .index(indexName),
      RequestOptions.DEFAULT
    )
  }

  private fun destroyIndex() {
    log.debug("Dropping offender index")
    if (esClient.indices().exists(GetIndexRequest(indexName), RequestOptions.DEFAULT)) {
      esClient.indices().delete(DeleteIndexRequest(indexName), RequestOptions.DEFAULT)
    }
  }

  private fun buildIndex() {
    esClient.indices().putTemplate(
      PutIndexTemplateRequest(templateName)
        .source("/elasticsearchdata/create-template.json".resourceAsString(), JSON),
      RequestOptions.DEFAULT
    )
    esClient.indices().create(CreateIndexRequest(indexName), RequestOptions.DEFAULT)
    log.debug("Build index")
  }

  private fun buildPipeline() {
    log.debug("Build pipeline")
    esClient.ingest()
      .putPipeline(PutPipelineRequest("person-search-pipeline", "/elasticsearchdata/create-pipeline.json".resourceAsByteReference(), JSON), RequestOptions.DEFAULT)
  }
}

private fun String.resourceAsString() = LocalStackHelper::class.java.getResource(this).readText()
private fun String.resourceAsByteReference() = BytesArray(LocalStackHelper::class.java.getResource(this).readBytes())
