package uk.gov.justice.hmpps.offendersearch.util

import com.amazonaws.util.IOUtils
import com.google.gson.JsonParser
import org.apache.http.HttpHeaders
import org.apache.http.entity.StringEntity
import org.apache.http.message.BasicHeader
import org.elasticsearch.client.ResponseException
import org.elasticsearch.client.RestHighLevelClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

class LocalStackHelper(var esClient: RestHighLevelClient) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun loadData() {
    destroyIndex()
    destroyPipeline()
    buildPipeline()
    buildIndex()
    loadOffender("1", loadFile("src/test/resources/elasticsearchdata/john-smith.json"))
    loadOffender("2", loadFile("src/test/resources/elasticsearchdata/jane-smith.json"))
    loadOffender("3", loadFile("src/test/resources/elasticsearchdata/sam-jones-deleted.json"))
    loadOffender("4", loadFile("src/test/resources/elasticsearchdata/antonio-gramsci-n01.json"))
    loadOffender("5", loadFile("src/test/resources/elasticsearchdata/antonio-gramsci-n02.json"))
    loadOffender("6", loadFile("src/test/resources/elasticsearchdata/antonio-gramsci-n03.json"))
    loadOffender("7", loadFile("src/test/resources/elasticsearchdata/anne-gramsci-n02.json"))
    loadOffender("8", loadFile("src/test/resources/elasticsearchdata/antonio-gramsci-c20.json"))
    waitForOffenderLoading()
  }

  private fun waitForOffenderLoading() {
    var count = 0
    while (count < 8) { //check offenders have been loaded before continuing with the test
      val something = esClient.lowLevelClient.performRequest("get", "/offender/_count", BasicHeader("any", "any"))
      count = JsonParser.parseString(IOUtils.toString(something.entity.content)).asJsonObject["count"].asInt
      log.debug("Offenders loaded count: {}", count)
      try {
        Thread.sleep(500)
      } catch (e: InterruptedException) {
        e.printStackTrace()
      }
    }
  }

  @Throws(IOException::class)
  private fun loadOffender(key: String, offender: String) {
    log.debug("Loading offender: {}", offender)
    esClient.lowLevelClient.performRequest("put", "/offender/document/$key?pipeline=pnc-pipeline", HashMap(), StringEntity(offender), BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json"))
  }

  @Throws(IOException::class)
  private fun destroyIndex() {
    log.debug("Dropping offender index")
    try {
      esClient.lowLevelClient.performRequest("delete", "/offender", BasicHeader("any", "any"))
    } catch (e: ResponseException) {
      log.debug("destroyIndex returned ", e)
    }
  }

  @Throws(IOException::class)
  private fun destroyPipeline() {
    try {
      log.debug("destroy pipeline")
      esClient.lowLevelClient.performRequest("delete", "/_ingest/pipeline/pnc-pipeline", BasicHeader("any", "any"))
    } catch (e: ResponseException) {
      log.debug("destroyPipeline returned ", e)
    }
  }

  @Throws(IOException::class)
  private fun buildIndex() {
    log.debug("Build index")
    esClient.lowLevelClient.performRequest("put", "/offender", HashMap(), StringEntity(loadFile("src/test/resources/elasticsearchdata/create-index.json")), BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json"))
  }

  @Throws(IOException::class)
  private fun buildPipeline() {
    log.debug("Build pipeline")
    esClient.lowLevelClient.performRequest("put", "/_ingest/pipeline/pnc-pipeline", HashMap(), StringEntity(loadFile("src/test/resources/elasticsearchdata/create-pipeline.json")), BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json"))
  }

  @Throws(IOException::class)
  private fun loadFile(file: String): String {
    return Files.readString(Paths.get(file))
  }

}