package uk.gov.justice.hmpps.offendersearch.health

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_CLASS
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.hmpps.offendersearch.wiremock.CommunityApiExtension
import uk.gov.justice.hmpps.offendersearch.wiremock.ElasticSearchExtension


@ExtendWith(CommunityApiExtension::class, ElasticSearchExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["test"])
@DirtiesContext(classMode = BEFORE_CLASS)
class HealthCheckTest {
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Test
  fun `Health page reports ok`() {
    stubPingWithResponse(200)

    webTestClient.get()
        .uri("/health")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("components.communityApiHealth.details.HttpStatus").isEqualTo("OK")
        .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `Health ping page is accessible`() {
    stubPingWithResponse(200)

    webTestClient.get()
        .uri("/health/ping")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `readiness reports ok`() {
    stubPingWithResponse(200)

    webTestClient.get()
        .uri("/health/readiness")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `liveness reports ok`() {
    stubPingWithResponse(200)

    webTestClient.get()
        .uri("/health/liveness")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `Health page reports down`() {
    stubPingWithResponse(404)

    webTestClient.get()
        .uri("/health")
        .exchange()
        .expectStatus()
        .is5xxServerError
        .expectBody()
        .jsonPath("status").isEqualTo("DOWN")
        .jsonPath("components.communityApiHealth.details.HttpStatus").isEqualTo("NOT_FOUND")
  }

  @Test
  fun `Health page reports a teapot`() {
    stubPingWithResponse(418)

    webTestClient.get()
        .uri("/health")
        .exchange()
        .expectStatus()
        .is5xxServerError
        .expectBody()
        .jsonPath("components.communityApiHealth.details.HttpStatus").isEqualTo("I_AM_A_TEAPOT")
        .jsonPath("status").isEqualTo("DOWN")
  }


  private fun stubPingWithResponse(status: Int) {
    CommunityApiExtension.communityApi.stubHealthPing(status)
    ElasticSearchExtension.elasticSearch.stubHealth(status)
  }

}
