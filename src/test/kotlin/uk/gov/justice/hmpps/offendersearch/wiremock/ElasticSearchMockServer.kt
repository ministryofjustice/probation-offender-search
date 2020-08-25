package uk.gov.justice.hmpps.offendersearch.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class ElasticSearchExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val elasticSearch = ElasticSearchMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    elasticSearch.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    elasticSearch.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    elasticSearch.stop()
  }
}

class ElasticSearchMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 4444
  }

  fun stubSearch( response: String) {
    stubFor(WireMock.post(WireMock.anyUrl()).willReturn(
        WireMock.okForContentType("application/json", response)))
  }

  fun stubHealth(status: Int) {
    stubFor(get("/_cluster/health/").willReturn(aResponse()
        .withBody("""
          {
            "cluster_name":"elasticsearch",
            "status":"yellow",
            "timed_out":false,
            "number_of_nodes":1,
            "number_of_data_nodes":1,
            "active_primary_shards":1,
            "active_shards":1,
            "relocating_shards":0,
            "initializing_shards":0,
            "unassigned_shards":1,
            "delayed_unassigned_shards":0,
            "number_of_pending_tasks":0,
            "number_of_in_flight_fetch":0,
            "task_max_waiting_in_queue_millis":0,
            "active_shards_percent_as_number":50.0
          }          
        """.trimIndent())
        .withHeader("content-type", "application/json; charset=UTF-8")
        .withStatus(status)))

  }
}

