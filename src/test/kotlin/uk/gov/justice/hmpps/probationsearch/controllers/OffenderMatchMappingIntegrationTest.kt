package uk.gov.justice.hmpps.probationsearch.controllers

import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import uk.gov.justice.hmpps.probationsearch.dto.MatchRequest
import java.time.LocalDate

@TestPropertySource(
  properties = [
    "search.supported.mapping.version=2",
  ],
)
@DirtiesContext
internal class OffenderMatchMappingIntegrationTest : OffenderMatchAPIIntegrationBase() {
  @BeforeEach
  internal fun setup() {
    loadOffenders(
      OffenderIdentification(
        surname = "Adorno",
        firstName = "Theodor",
        dateOfBirth = LocalDate.of(1903, 11, 11),
        crn = "X00001",
        croNumber = "AA99/655108T",
        pncNumber = "2015/0123456X",
        aliases = listOf(
          Alias(firstName = "Nicola", surname = "Abbagnano", dateOfBirth = LocalDate.of(1990, 9, 1)),
          Alias(firstName = "Bhimrao", surname = "Ambedkar", dateOfBirth = LocalDate.of(1891, 4, 14)),
        ),
      ),
    )
  }

  @Nested
  inner class CRONumberMatching {
    @Test
    internal fun `will only partial match for a particular alias and date of birth`() {
      given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body(
          MatchRequest(
            surname = "Abbagnano",
            firstName = "Bhimrao",
            dateOfBirth = LocalDate.of(1891, 4, 14),
            croNumber = "AA99/655108T",
          ),
        )
        .post("/match")
        .then()
        .statusCode(200)
        .body("matchedBy", equalTo("EXTERNAL_KEY"))
        .body("matches.findall.size()", equalTo(1))
    }

    @Test
    internal fun `will match for a particular name and date of birth`() {
      given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body(
          MatchRequest(
            surname = "Adorno",
            firstName = "Theodor",
            dateOfBirth = LocalDate.of(1903, 11, 11),
            croNumber = "AA99/655108T",
          ),
        )
        .post("/match")
        .then()
        .statusCode(200)
        .body("matchedBy", equalTo("ALL_SUPPLIED"))
        .body("matches.findall.size()", equalTo(1))
    }

    @Test
    internal fun `will match for a particular alias and date of birth`() {
      given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body(
          MatchRequest(
            surname = "Ambedkar",
            firstName = "Bhimrao",
            dateOfBirth = LocalDate.of(1891, 4, 14),
            croNumber = "AA99/655108T",
          ),
        )
        .post("/match")
        .then()
        .statusCode(200)
        .body("matchedBy", equalTo("ALL_SUPPLIED_ALIAS"))
        .body("matches.findall.size()", equalTo(1))
    }

    @Test
    internal fun `will partial match for a particular alias and date of birth`() {
      given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body(
          MatchRequest(
            surname = "Ambedkar",
            firstName = "Bhimrao",
            dateOfBirth = LocalDate.of(1999, 4, 14),
            croNumber = "AA99/655108T",
          ),
        )
        .post("/match")
        .then()
        .statusCode(200)
        .body("matchedBy", equalTo("EXTERNAL_KEY"))
        .body("matches.findall.size()", equalTo(1))
    }
  }

  @Nested
  inner class PNCNumberMatching {
    @Test
    internal fun `will only partial match for a particular alias and date of birth`() {
      given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body(
          MatchRequest(
            surname = "Abbagnano",
            firstName = "Bhimrao",
            dateOfBirth = LocalDate.of(1891, 4, 14),
            pncNumber = "2015/0123456X",
          ),
        )
        .post("/match")
        .then()
        .statusCode(200)
        .body("matchedBy", equalTo("EXTERNAL_KEY"))
        .body("matches.findall.size()", equalTo(1))
    }

    @Test
    internal fun `will match for a particular name and date of birth`() {
      given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body(
          MatchRequest(
            surname = "Adorno",
            firstName = "Theodor",
            dateOfBirth = LocalDate.of(1903, 11, 11),
            pncNumber = "2015/0123456X",
          ),
        )
        .post("/match")
        .then()
        .statusCode(200)
        .body("matchedBy", equalTo("ALL_SUPPLIED"))
        .body("matches.findall.size()", equalTo(1))
    }

    @Test
    internal fun `will match for a particular alias and date of birth`() {
      given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body(
          MatchRequest(
            surname = "Ambedkar",
            firstName = "Bhimrao",
            dateOfBirth = LocalDate.of(1891, 4, 14),
            pncNumber = "2015/0123456X",
          ),
        )
        .post("/match")
        .then()
        .statusCode(200)
        .body("matchedBy", equalTo("ALL_SUPPLIED_ALIAS"))
        .body("matches.findall.size()", equalTo(1))
    }

    @Test
    internal fun `will partial match for a particular alias and date of birth`() {
      given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body(
          MatchRequest(
            surname = "Ambedkar",
            firstName = "Bhimrao",
            dateOfBirth = LocalDate.of(1999, 4, 14),
            pncNumber = "2015/0123456X",
          ),
        )
        .post("/match")
        .then()
        .statusCode(200)
        .body("matchedBy", equalTo("EXTERNAL_KEY"))
        .body("matches.findall.size()", equalTo(1))
    }
  }

  @Nested
  inner class NameMatching {
    @Test
    internal fun `will not cross match across aliases and date of births`() {
      given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body(
          MatchRequest(
            surname = "Abbagnano",
            firstName = "Bhimrao",
            dateOfBirth = LocalDate.of(1891, 4, 14),
            croNumber = "SF80/655108T",
          ),
        )
        .post("/match")
        .then()
        .statusCode(200)
        .body("matchedBy", equalTo("NOTHING"))
        .body("matches.findall.size()", equalTo(0))
    }

    @Test
    internal fun `will match for a particular name and date of birth`() {
      given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body(
          MatchRequest(
            surname = "Adorno",
            firstName = "Theodor",
            dateOfBirth = LocalDate.of(1903, 11, 11),
            croNumber = "SF80/655108T",
          ),
        )
        .post("/match")
        .then()
        .statusCode(200)
        .body("matchedBy", equalTo("NAME"))
        .body("matches.findall.size()", equalTo(1))
    }

    @Test
    internal fun `will match for a particular alias and date of birth`() {
      given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body(
          MatchRequest(
            surname = "Ambedkar",
            firstName = "Bhimrao",
            dateOfBirth = LocalDate.of(1891, 4, 14),
            croNumber = "SF80/655108T",
          ),
        )
        .post("/match")
        .then()
        .statusCode(200)
        .body("matchedBy", equalTo("NAME"))
        .body("matches.findall.size()", equalTo(1))
    }
  }
}
