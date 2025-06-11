package uk.gov.justice.hmpps.probationsearch.util

import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.opensearch.action.admin.indices.delete.DeleteIndexRequest
import org.opensearch.action.index.IndexRequest
import org.opensearch.action.ingest.PutPipelineRequest
import org.opensearch.client.RequestOptions
import org.opensearch.client.RestHighLevelClient
import org.opensearch.client.core.CountRequest
import org.opensearch.client.indices.CreateIndexRequest
import org.opensearch.client.indices.GetIndexRequest
import org.opensearch.client.indices.PutIndexTemplateRequest
import org.opensearch.common.xcontent.XContentType.JSON
import org.opensearch.core.common.bytes.BytesArray
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class PersonSearchHelper(private val openSearchClient: RestHighLevelClient) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
    const val indexName = "person-search-primary"
    const val templateName = "person-search-template"
  }

  fun loadData() {
    rebuildIndex()
    loadOffender("1", "/search-data/john-smith.json".resourceAsString())
    loadOffender("2", "/search-data/jane-smith.json".resourceAsString())
    loadOffender("3", "/search-data/sam-jones-deleted.json".resourceAsString())
    loadOffender("4", "/search-data/antonio-gramsci-n01.json".resourceAsString())
    loadOffender("5", "/search-data/antonio-gramsci-n02.json".resourceAsString())
    loadOffender("6", "/search-data/antonio-gramsci-n03.json".resourceAsString())
    loadOffender("7", "/search-data/anne-gramsci-n02.json".resourceAsString())
    loadOffender("8", "/search-data/antonio-gramsci-c20.json".resourceAsString())
    loadOffender("9", "/search-data/tom-bloggs.json".resourceAsString())
    loadOffender("10", "/search-data/james-brown.json".resourceAsString())
    waitForOffenderLoading(10)
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
      openSearchClient.count(CountRequest(indexName), RequestOptions.DEFAULT).count
    } matches { it == expectedCount.toLong() }
  }

  private fun loadOffender(key: String, offender: String) {
    openSearchClient.index(
      IndexRequest()
        .source(offender, JSON)
        .id(key)
        .index(indexName),
      RequestOptions.DEFAULT,
    )
  }

  private fun destroyIndex() {
    if (openSearchClient.indices().exists(GetIndexRequest(indexName), RequestOptions.DEFAULT)) {
      openSearchClient.indices().delete(DeleteIndexRequest(indexName), RequestOptions.DEFAULT)
    }
  }

  private fun buildIndex() {
    openSearchClient.indices().putTemplate(
      PutIndexTemplateRequest(templateName)
        .source("/search-setup/person-search-index-template.json".resourceAsString(), JSON),
      RequestOptions.DEFAULT,
    )
    openSearchClient.indices().create(CreateIndexRequest(indexName), RequestOptions.DEFAULT)
  }

  private fun buildPipeline() {
    openSearchClient.ingest()
      .putPipeline(
        PutPipelineRequest(
          "person-search-pipeline",
          "/search-setup/person-search-pipeline.json".resourceAsByteReference(),
          JSON,
        ),
        RequestOptions.DEFAULT,
      )
  }
}

private fun String.resourceAsString() = PersonSearchHelper::class.java.getResource(this)!!.readText()
private fun String.resourceAsByteReference() =
  BytesArray(PersonSearchHelper::class.java.getResource(this)!!.readBytes())
