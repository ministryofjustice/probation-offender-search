package uk.gov.justice.hmpps.offendersearch.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit.WireMockRule
import io.restassured.RestAssured
import io.restassured.config.ObjectMapperConfig
import io.restassured.config.RestAssuredConfig
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers
import org.junit.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import uk.gov.justice.hmpps.offendersearch.dto.OffenderDetail
import java.lang.reflect.Type
import java.nio.file.Files
import java.nio.file.Paths

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev,wiremock")
@RunWith(SpringJUnit4ClassRunner::class)
class OffenderSearchAPITest {
  @LocalServerPort
  var port = 0
  @Autowired
  private lateinit var objectMapper: ObjectMapper
  @Value("\${test.token.good}")
  private val validOauthToken: String? = null

  companion object {
    private val wireMock = WireMockRule(WireMockConfiguration.wireMockConfig().port(4444).jettyStopTimeout(10000L))

    @BeforeClass
    @JvmStatic
    fun startMocks() {
      wireMock.start()
    }

    @AfterClass
    @JvmStatic
    fun stopMocks() {
      wireMock.stop()
    }
  }
  @Before
  fun setup() {
    RestAssured.port = port
    RestAssured.config = RestAssuredConfig.config().objectMapperConfig(
        ObjectMapperConfig().jackson2ObjectMapperFactory { _: Type?, _: String? -> objectMapper })
  }

  @Test
  fun offenderSearch() {
    wireMock.stubFor(WireMock.get(WireMock.anyUrl()).willReturn(
        WireMock.okForContentType("application/json", response("src/test/resources/elasticsearchdata/singleMatch.json"))))
    val results = RestAssured.given()
        .auth()
        .oauth2(validOauthToken)
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
        .oauth2(validOauthToken)
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
        .oauth2(validOauthToken)
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("{\"dateOfBirth\":\"23/11/1976\"}")
        .`when`()["/search"]
        .then()
        .statusCode(400)
  }

  private fun response(file: String): String {
    return Files.readString(Paths.get(file))
  }
}