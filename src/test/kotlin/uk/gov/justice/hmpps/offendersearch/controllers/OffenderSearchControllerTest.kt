package uk.gov.justice.hmpps.offendersearch.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.verify
import io.restassured.RestAssured
import io.restassured.config.ObjectMapperConfig
import io.restassured.config.RestAssuredConfig
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.hmpps.offendersearch.dto.OffenderDetail
import uk.gov.justice.hmpps.offendersearch.util.JwtAuthenticationHelper
import uk.gov.justice.hmpps.offendersearch.wiremock.ElasticSearchExtension
import java.lang.reflect.Type
import java.nio.file.Files
import java.nio.file.Paths

@ExtendWith(ElasticSearchExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["test", "wiremock"])
class OffenderSearchControllerTest {
  @LocalServerPort
  var port = 0

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Autowired
  private lateinit var jwtAuthenticationHelper: JwtAuthenticationHelper

  @SpyBean
  lateinit var telemetryClient: TelemetryClient

  @BeforeEach
  fun setup() {
    RestAssured.port = port
    RestAssured.config = RestAssuredConfig.config().objectMapperConfig(
      ObjectMapperConfig().jackson2ObjectMapperFactory { _: Type?, _: String? -> objectMapper }
    )
  }

  @Test
  fun offenderSearch() {
    ElasticSearchExtension.elasticSearch.stubSearch(response("src/test/resources/elasticsearchdata/singleMatch.json"))
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
    assertThat(results).hasSize(1)
    assertThat(results).extracting("firstName").containsOnly("John")
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
      ElasticSearchExtension.elasticSearch.stubSearch(response("src/test/resources/elasticsearchdata/singleMatch.json"))
      RestAssured.given()
        .get("/synthetic-monitor")
        .then()
        .statusCode(200)
    }

    @Test
    fun `telemetry is recorded`() {
      ElasticSearchExtension.elasticSearch.stubSearch(response("src/test/resources/elasticsearchdata/singleMatch.json"))
      RestAssured.given()
        .get("/synthetic-monitor")
        .then()
        .statusCode(200)

      verify(telemetryClient).trackEvent(
        eq("synthetic-monitor"),
        com.nhaarman.mockitokotlin2.check<Map<String, String>> {
          assertThat(it["results"]).containsOnlyDigits()
          assertThat(it["timeMs"]).containsOnlyDigits()
        },
        isNull()
      )
    }
  }
}
