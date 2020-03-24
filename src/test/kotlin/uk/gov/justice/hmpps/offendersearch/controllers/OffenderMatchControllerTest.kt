package uk.gov.justice.hmpps.offendersearch.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import io.restassured.RestAssured
import io.restassured.config.ObjectMapperConfig
import io.restassured.config.RestAssuredConfig
import org.hamcrest.CoreMatchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.hmpps.offendersearch.dto.OffenderMatches
import uk.gov.justice.hmpps.offendersearch.services.MatchService
import uk.gov.justice.hmpps.offendersearch.util.JwtAuthenticationHelper
import java.lang.reflect.Type

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(SpringExtension::class)
internal class OffenderMatchControllerTest {
  @LocalServerPort
  var port = 0
  @Autowired
  private lateinit var objectMapper: ObjectMapper
  @Autowired
  private lateinit var jwtAuthenticationHelper: JwtAuthenticationHelper
  @MockBean
  private lateinit var matchService: MatchService

  @BeforeEach
  fun setUp() {
    RestAssured.port = port
    RestAssured.config = RestAssuredConfig.config().objectMapperConfig(
        ObjectMapperConfig().jackson2ObjectMapperFactory { _: Type?, _: String? -> objectMapper })
    whenever(matchService.match(any())).thenReturn(OffenderMatches(listOf()))
  }

  @Test
  fun `surname is mandatory`() {
    RestAssured.given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
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
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
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
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("{\"surname\": \"Smith\"}")
        .post("/match")
        .then()
        .statusCode(200)
  }

}
