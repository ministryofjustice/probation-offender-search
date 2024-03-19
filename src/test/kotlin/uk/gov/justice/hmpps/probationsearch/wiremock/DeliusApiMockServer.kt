package uk.gov.justice.hmpps.probationsearch.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.net.HttpURLConnection

class DeliusApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val deliusApi = DeliusApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    deliusApi.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    deliusApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    deliusApi.stop()
  }
}

class DeliusApiMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 9092
  }

  fun stubDeliusAuditSuccess() {
    stubFor(
      WireMock.post(urlPathEqualTo("/probation-search/audit/contact-search")).willReturn(
        WireMock.aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(HttpURLConnection.HTTP_CREATED)
      ),
    )
  }
}
