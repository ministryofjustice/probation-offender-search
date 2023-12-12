package uk.gov.justice.hmpps.probationsearch.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.net.HttpURLConnection

class CommunityApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val communityApi = CommunityApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    communityApi.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    communityApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    communityApi.stop()
  }
}

class CommunityApiMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 9091
  }

  fun stubHealthPing(status: Int) {
    stubFor(
      get("/health/ping").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(if (status == 200) "pong" else "some error")
          .withStatus(status),
      ),
    )
  }

  fun stubUserAccess(crn: String, response: String) {
    stubFor(
      get("/secure/offenders/crn/$crn/userAccess").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(response)
          .withStatus(200),
      ),
    )
  }

  fun stubUserAccessDenied(crn: String, response: String) {
    stubFor(
      get("/secure/offenders/crn/$crn/userAccess").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(response)
          .withStatus(403),
      ),
    )
  }

  fun stubUserAccessNotFound(crn: String) {
    stubFor(
      get("/secure/offenders/crn/$crn/userAccess").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody("{\"error\": \"not found\"}")
          .withStatus(HttpURLConnection.HTTP_NOT_FOUND),
      ),
    )
  }

  fun stubUserAccessError(crn: String) {
    stubFor(
      get("/secure/offenders/crn/$crn/userAccess").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(HttpURLConnection.HTTP_INTERNAL_ERROR),
      ),
    )
  }
}
