package uk.gov.justice.hmpps.probationsearch.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import io.restassured.RestAssured
import io.restassured.config.ObjectMapperConfig
import io.restassured.config.RestAssuredConfig
import org.hamcrest.CoreMatchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.hmpps.probationsearch.dto.OffenderMatches
import uk.gov.justice.hmpps.probationsearch.services.FeatureFlags
import uk.gov.justice.hmpps.probationsearch.services.MatchService
import uk.gov.justice.hmpps.probationsearch.util.JwtAuthenticationHelper
import java.lang.reflect.Type

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@MockitoBean(types = [FeatureFlags::class])
@ActiveProfiles("test")
@ExtendWith(SpringExtension::class)
internal class OffenderMatchControllerTest {
  @LocalServerPort
  var port = 0

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Autowired
  private lateinit var jwtAuthenticationHelper: JwtAuthenticationHelper

  @MockitoBean
  private lateinit var matchService: MatchService

  @BeforeEach
  fun setUp() {
    RestAssured.port = port
    RestAssured.config = RestAssuredConfig.config().objectMapperConfig(
      ObjectMapperConfig().jackson2ObjectMapperFactory { _: Type?, _: String? -> objectMapper },
    )
    whenever(matchService.match(any())).thenReturn(OffenderMatches(listOf()))
  }

  @Test
  fun `surname is mandatory`() {
    RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_PROBATION__SEARCH_PERSON"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("{}")
      .post("/match")
      .then()
      .statusCode(400)
      .body("developerMessage", containsString("Surname is required"))
  }

  @Test
  fun `date of birth must be in the past`() {
    RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_PROBATION__SEARCH_PERSON"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("{\"surname\": \"Smith\", \"dateOfBirth\":\"2199-07-02\"}")
      .post("/match")
      .then()
      .statusCode(400)
      .body("developerMessage", containsString("Date of birth must be in the past"))
  }

  @Test
  fun `OK response with valid request`() {
    RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_PROBATION__SEARCH_PERSON"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("{\"surname\": \"Smith\"}")
      .post("/match")
      .then()
      .statusCode(200)
  }
}
