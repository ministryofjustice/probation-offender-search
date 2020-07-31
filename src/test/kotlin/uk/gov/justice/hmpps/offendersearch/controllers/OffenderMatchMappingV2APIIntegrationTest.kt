package uk.gov.justice.hmpps.offendersearch.controllers

import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import uk.gov.justice.hmpps.offendersearch.dto.MatchRequest
import java.time.LocalDate

@TestPropertySource(properties = [
  "search.supported.mapping.version=2"
])
@DirtiesContext
internal class OffenderMatchMappingV2APIIntegrationTest : OffenderMatchAPIIntegrationBase() {
  @BeforeEach
  internal fun setup() {
    loadOffenders(
        OffenderIdentification(
            surname = "Adorno",
            firstName = "Theodor",
            dateOfBirth = LocalDate.of(1903, 11, 11),
            crn = "X00001",
            aliases = listOf(
                Alias(firstName = "Nicola", surname = "Abbagnano", dateOfBirth = LocalDate.of(1990, 9, 1)),
                Alias(firstName = "Bhimrao", surname = "Ambedkar", dateOfBirth = LocalDate.of(1891, 4, 14))
            )
        )
    )
  }

  @Test
  internal fun `will not cross match across aliases and date of births`() {
    given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body(MatchRequest(
            surname = "Abbagnano",
            firstName = "Bhimrao",
            dateOfBirth = LocalDate.of(1891, 4, 14),
            croNumber = "SF80/655108T"
        ))
        .post("/match")
        .then()
        .statusCode(200)
        .body("matchedBy", equalTo("NOTHING"))
        .body("matches.findall.size()", equalTo(0))
  }

  @Test
  internal fun `will match for a particular alias and date of birth`() {
    given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body(MatchRequest(
            surname = "Ambedkar",
            firstName = "Bhimrao",
            dateOfBirth = LocalDate.of(1891, 4, 14),
            croNumber = "SF80/655108T"
        ))
        .post("/match")
        .then()
        .statusCode(200)
        .body("matchedBy", equalTo("NAME"))
        .body("matches.findall.size()", equalTo(1))
  }

}





