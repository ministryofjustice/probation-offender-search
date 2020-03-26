package uk.gov.justice.hmpps.offendersearch.util

import com.amazonaws.util.IOUtils
import com.google.gson.JsonParser
import org.apache.http.HttpHeaders
import org.apache.http.entity.StringEntity
import org.apache.http.message.BasicHeader
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.elasticsearch.client.ResponseException
import org.elasticsearch.client.RestHighLevelClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

class LocalStackHelper(var esClient: RestHighLevelClient) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun loadData() {
    rebuildIndex()
    loadOffender("1", loadFile("src/test/resources/elasticsearchdata/john-smith.json"))
    loadOffender("2", loadFile("src/test/resources/elasticsearchdata/jane-smith.json"))
    loadOffender("3", loadFile("src/test/resources/elasticsearchdata/sam-jones-deleted.json"))
    loadOffender("4", loadFile("src/test/resources/elasticsearchdata/antonio-gramsci-n01.json"))
    loadOffender("5", loadFile("src/test/resources/elasticsearchdata/antonio-gramsci-n02.json"))
    loadOffender("6", loadFile("src/test/resources/elasticsearchdata/antonio-gramsci-n03.json"))
    loadOffender("7", loadFile("src/test/resources/elasticsearchdata/anne-gramsci-n02.json"))
    loadOffender("8", loadFile("src/test/resources/elasticsearchdata/antonio-gramsci-c20.json"))
    waitForOffenderLoading(8)
  }

  fun loadData(offenders : List<String>) {
    rebuildIndex()

    offenders.forEach {
      loadOffender(UUID.randomUUID().toString(), it)
    }

    waitForOffenderLoading(offenders.size)
  }

  private fun rebuildIndex() {
    destroyIndex()
    destroyPipeline()
    buildPipeline()
    buildIndex()
  }

  private fun waitForOffenderLoading(expectedCount: Int) {
    await untilCallTo {
      val response = esClient.lowLevelClient.performRequest("get", "/offender/_count", BasicHeader("any", "any"))
      JsonParser.parseString(IOUtils.toString(response.entity.content)).asJsonObject["count"].asInt
    } matches { it == expectedCount }
  }

  private fun loadOffender(key: String, offender: String) {
    log.debug("Loading offender: {}", offender)
    esClient.lowLevelClient.performRequest("put", "/offender/document/$key?pipeline=pnc-pipeline", HashMap(), StringEntity(offender), BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json"))
  }

  private fun destroyIndex() {
    log.debug("Dropping offender index")
    try {
      esClient.lowLevelClient.performRequest("delete", "/offender", BasicHeader("any", "any"))
    } catch (e: ResponseException) {
      log.debug("destroyIndex returned ", e)
    }
  }

  private fun destroyPipeline() {
    try {
      log.debug("destroy pipeline")
      esClient.lowLevelClient.performRequest("delete", "/_ingest/pipeline/pnc-pipeline", BasicHeader("any", "any"))
    } catch (e: ResponseException) {
      log.debug("destroyPipeline returned ", e)
    }
  }

  private fun buildIndex() {
    log.debug("Build index")
    esClient.lowLevelClient.performRequest("put", "/offender", HashMap(), StringEntity(loadFile("src/test/resources/elasticsearchdata/create-index.json")), BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json"))
  }

  private fun buildPipeline() {
    log.debug("Build pipeline")
    esClient.lowLevelClient.performRequest("put", "/_ingest/pipeline/pnc-pipeline", HashMap(), StringEntity(loadFile("src/test/resources/elasticsearchdata/create-pipeline.json")), BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json"))
  }

  private fun loadFile(file: String): String {
    return Files.readString(Paths.get(file))
  }

}