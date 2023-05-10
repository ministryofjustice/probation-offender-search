package uk.gov.justice.hmpps.probationsearch.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import io.restassured.RestAssured
import io.restassured.config.ObjectMapperConfig
import io.restassured.config.RestAssuredConfig
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.opensearch.client.RestHighLevelClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.hmpps.probationsearch.dto.OffenderDetail
import uk.gov.justice.hmpps.probationsearch.util.JwtAuthenticationHelper
import uk.gov.justice.hmpps.probationsearch.util.PersonSearchHelper
import uk.gov.justice.hmpps.probationsearch.wiremock.OpenSearchExtension
import java.lang.reflect.Type
import java.nio.file.Files
import java.nio.file.Paths

@ExtendWith(OpenSearchExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["test", "wiremock"])
class OffenderSearchControllerTest {
  @LocalServerPort
  var port = 0

  @Autowired
  @Qualifier("openSearchClient")
  internal lateinit var openSearchClient: RestHighLevelClient

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Autowired
  private lateinit var jwtAuthenticationHelper: JwtAuthenticationHelper

  @SpyBean
  lateinit var telemetryClient: TelemetryClient

  @BeforeEach
  fun setup() {
    PersonSearchHelper(openSearchClient).loadData()
    RestAssured.port = port
    RestAssured.config = RestAssuredConfig.config().objectMapperConfig(
      ObjectMapperConfig().jackson2ObjectMapperFactory { _: Type?, _: String? -> objectMapper },
    )
  }

  @Test
  fun offenderSearch() {
    OpenSearchExtension.openSearch.stubSearch(response("src/test/resources/searchdata/singleMatch.json"))
    val results = RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("{\"surname\":\"smith\"}")
      .`when`()["/search"]
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(Array<OffenderDetail>::class.java)
    assertThat(results).hasSize(2)
    assertThat(results).extracting("firstName").containsExactlyInAnyOrder("John", "Jane")
  }

  @Test
  fun noSearchParameters_badRequest() {
    RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("{}")
      .`when`()["/search"]
      .then()
      .statusCode(400)
      .body("developerMessage", CoreMatchers.containsString("Invalid search  - please provide at least 1 search parameter"))
  }

  @Test
  fun invalidDateOfBirthFormat_badRequest() {
    RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("{\"dateOfBirth\":\"23/11/1976\"}")
      .`when`()["/search"]
      .then()
      .statusCode(400)
  }

  private fun response(file: String): String {
    return Files.readString(Paths.get(file))
  }

  @Nested
  inner class SyntheticMonitor {

    @Test
    fun `endpoint is unsecured`() {
      OpenSearchExtension.openSearch.stubSearch(response("src/test/resources/searchdata/singleMatch.json"))
      RestAssured.given()
        .get("/synthetic-monitor")
        .then()
        .statusCode(200)
    }

    @Test
    fun `telemetry is recorded`() {
      OpenSearchExtension.openSearch.stubSearch(response("src/test/resources/searchdata/singleMatch.json"))
      RestAssured.given()
        .get("/synthetic-monitor")
        .then()
        .statusCode(200)

      verify(telemetryClient).trackEvent(
        eq("synthetic-monitor"),
        check<Map<String, String>> {
          assertThat(it["results"]).containsOnlyDigits()
          assertThat(it["timeMs"]).containsOnlyDigits()
        },
        isNull(),
      )
    }
  }
}
