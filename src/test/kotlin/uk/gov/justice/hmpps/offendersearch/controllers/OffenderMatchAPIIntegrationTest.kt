package uk.gov.justice.hmpps.offendersearch.controllers

import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.hmpps.offendersearch.dto.MatchRequest
import java.time.LocalDate

internal class OffenderMatchControllerAPIIntegrationTest : OffenderMatchAPIIntegrationBase() {
  @Nested
  inner class BasicOperation {
    @Test
    internal fun `access allowed with ROLE_COMMUNITY`() {
      loadOffenders()
      given()
          .auth()
          .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .body("{\"surname\": \"Smith\"}")
          .post("/match")
          .then()
          .statusCode(200)
    }

    @Test
    internal fun `without ROLE_COMMUNITY access is denied`() {
      given()
          .auth()
          .oauth2(jwtAuthenticationHelper.createJwt("ROLE_BINGO"))
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .body("{\"surname\": \"Smith\"}")
          .post("/match")
          .then()
          .statusCode(403)
    }

    @Test
    internal fun `should match when a single offender has all matching attributes`() {
      loadOffenders(
          OffenderIdentification(
              surname = "gramsci",
              firstName = "anne",
              dateOfBirth = LocalDate.of(1988, 1, 6),
              crn = "X00007",
              nomsNumber = "G5555TT",
              croNumber = "SF80/655108T",
              pncNumber = "2018/0123456X"
          ),
          OffenderIdentification(
              surname = "smith",
              firstName = "john",
              dateOfBirth = LocalDate.of(1921, 1, 6),
              crn = "X00001"
          )
      )

      given()
          .auth()
          .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .body(MatchRequest(
              surname = "gramsci",
              firstName = "anne",
              dateOfBirth = LocalDate.of(1988, 1, 6),
              nomsNumber = "G5555TT",
              croNumber = "SF80/655108T",
              pncNumber = "2018/0123456X",
              activeSentence = true
          ))
          .post("/match")
          .then()
          .statusCode(200)
          .body("matchedBy", equalTo("ALL_SUPPLIED"))
          .body("matches.findall.size()", equalTo(1))
          .body("matches[0].offender.otherIds.crn", equalTo("X00007"))
    }
  }

  @Nested
  inner class PNCNumberMatching {
    @BeforeEach
    internal fun setup() {
      loadOffenders(
          OffenderIdentification(
              surname = "gramsci",
              firstName = "jan",
              dateOfBirth = LocalDate.of(1988, 1, 6),
              crn = "X00001",
              pncNumber = "2015/0123456X",
              aliases = listOf(Alias(firstName = "Nicola", surname = "Abbagnano", dateOfBirth = LocalDate.of(1990, 9, 1)))
          ),
          OffenderIdentification(
              surname = "gramsci",
              firstName = "jan",
              dateOfBirth = LocalDate.of(1988, 1, 6),
              crn = "X888888",
              pncNumber = "2015/0123456X",
              deleted = true
          ),
          OffenderIdentification(
              surname = "gramsci",
              firstName = "june",
              dateOfBirth = LocalDate.of(1988, 1, 6),
              crn = "X999999",
              pncNumber = "2015/0123456X",
              activeSentence = false
          ),
          OffenderIdentification(
              surname = "gramsci",
              firstName = "jill",
              dateOfBirth = LocalDate.of(1988, 1, 6),
              crn = "X00002",
              pncNumber = "2015/0123456Z",
              nomsNumber = "G5555TT"
          )
      )
    }

    @Test
    internal fun `should match using PNC number ignoring deleted and inactive sentences`() {
      given()
          .auth()
          .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .body(MatchRequest(
              surname = "gramsci",
              firstName = "june",
              dateOfBirth = LocalDate.of(1988, 1, 6),
              pncNumber = "2015/0123456X",
              activeSentence = true
          ))
          .post("/match")
          .then()
          .statusCode(200)
          .body("matchedBy", equalTo("EXTERNAL_KEY"))
          .body("matches.findall.size()", equalTo(1))
          .body("matches[0].offender.otherIds.crn", equalTo("X00001"))
    }

    @Test
    internal fun `should match using short form of PNC number`() {
      given()
          .auth()
          .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .body(MatchRequest(
              surname = "gramsci",
              firstName = "june",
              dateOfBirth = LocalDate.of(1988, 1, 6),
              pncNumber = "15/123456X",
              activeSentence = true
          ))
          .post("/match")
          .then()
          .statusCode(200)
          .body("matches.findall.size()", equalTo(1))
          .body("matches[0].offender.otherIds.crn", equalTo("X00001"))
    }

    @Test
    internal fun `noms number takes precedence over PNC Number when present`() {
      given()
          .auth()
          .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .body(MatchRequest(
              surname = "gramsci",
              firstName = "june",
              dateOfBirth = LocalDate.of(1988, 1, 6),
              pncNumber = "2015/0123456X",
              activeSentence = true,
              nomsNumber = "G5555TT"
          ))
          .post("/match")
          .then()
          .statusCode(200)
          .body("matchedBy", equalTo("HMPPS_KEY"))
          .body("matches.findall.size()", equalTo(1))
          .body("matches[0].offender.otherIds.crn", equalTo("X00002"))
    }

    @Test
    internal fun `should not match using PNC number if no other data matches the record other then PNC number`() {
      given()
          .auth()
          .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .body(MatchRequest(
              surname = "ahankara",
              firstName = "adi",
              dateOfBirth = LocalDate.of(1977, 1, 6),
              pncNumber = "2015/0123456X",
              activeSentence = true
          ))
          .post("/match")
          .then()
          .statusCode(200)
          .body("matches.findall.size()", equalTo(0))
    }

    @Test
    internal fun `should match using PNC number if PNC and surname match`() {
      given()
          .auth()
          .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .body(MatchRequest(
              surname = "gramsci",
              firstName = "june",
              dateOfBirth = LocalDate.of(1977, 1, 6),
              pncNumber = "2015/0123456X",
              activeSentence = true
          ))
          .post("/match")
          .then()
          .statusCode(200)
          .body("matches.findall.size()", equalTo(1))
          .body("matches[0].offender.otherIds.crn", equalTo("X00001"))
    }

    @Test
    internal fun `should match using PNC number if PNC and date of birth match`() {
      given()
          .auth()
          .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .body(MatchRequest(
              surname = "ahankara",
              firstName = "adi",
              dateOfBirth = LocalDate.of(1988, 1, 6),
              pncNumber = "2015/0123456X",
              activeSentence = true
          ))
          .post("/match")
          .then()
          .statusCode(200)
          .body("matches.findall.size()", equalTo(1))
          .body("matches[0].offender.otherIds.crn", equalTo("X00001"))
    }

    @Test
    internal fun `should match using PNC number if PNC and surname alias match`() {
      given()
          .auth()
          .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .body(MatchRequest(
              surname = "abbagnano",
              firstName = "june",
              dateOfBirth = LocalDate.of(1977, 1, 6),
              pncNumber = "2015/0123456X",
              activeSentence = true
          ))
          .post("/match")
          .then()
          .statusCode(200)
          .body("matches.findall.size()", equalTo(1))
          .body("matches[0].offender.otherIds.crn", equalTo("X00001"))
    }

    @Test
    internal fun `should match using PNC number if PNC and date of birth alias match`() {
      given()
          .auth()
          .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .body(MatchRequest(
              surname = "ahankara",
              firstName = "adi",
              dateOfBirth = LocalDate.of(1990, 9, 1),
              pncNumber = "2015/0123456X",
              activeSentence = true
          ))
          .post("/match")
          .then()
          .statusCode(200)
          .body("matches.findall.size()", equalTo(1))
          .body("matches[0].offender.otherIds.crn", equalTo("X00001"))
    }

  }

  @Nested
  inner class CRONumberMatching {
    @BeforeEach
    internal fun setup() {
      loadOffenders(
          OffenderIdentification(
              surname = "gramsci",
              firstName = "jan",
              dateOfBirth = LocalDate.of(1988, 1, 6),
              crn = "X00001",
              croNumber = "SF80/655108T",
              aliases = listOf(Alias(firstName = "Nicola", surname = "Abbagnano", dateOfBirth = LocalDate.of(1990, 9, 1)))
          ),
          OffenderIdentification(
              surname = "gramsci",
              firstName = "jan",
              dateOfBirth = LocalDate.of(1988, 1, 6),
              crn = "X888888",
              croNumber = "SF80/655108T",
              deleted = true
          ),
          OffenderIdentification(
              surname = "gramsci",
              firstName = "june",
              dateOfBirth = LocalDate.of(1988, 1, 6),
              crn = "X999999",
              croNumber = "SF80/655108T",
              activeSentence = false
          ),
          OffenderIdentification(
              surname = "gramsci",
              firstName = "jill",
              dateOfBirth = LocalDate.of(1988, 1, 6),
              crn = "X00002",
              croNumber = "SF79/666108T",
              nomsNumber = "G5555TT"
          )
      )
    }

    @Test
    internal fun `should match using CRO number ignoring deleted and inactive sentences`() {
      given()
          .auth()
          .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .body(MatchRequest(
              surname = "gramsci",
              firstName = "june",
              dateOfBirth = LocalDate.of(1988, 1, 6),
              croNumber = "SF80/655108T",
              activeSentence = true
          ))
          .post("/match")
          .then()
          .statusCode(200)
          .body("matchedBy", equalTo("EXTERNAL_KEY"))
          .body("matches.findall.size()", equalTo(1))
          .body("matches[0].offender.otherIds.crn", equalTo("X00001"))
    }

    @Test
    internal fun `noms number takes precedence over CRO Number when present`() {
      given()
          .auth()
          .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .body(MatchRequest(
              surname = "gramsci",
              firstName = "june",
              dateOfBirth = LocalDate.of(1988, 1, 6),
              croNumber = "SF80/655108T",
              activeSentence = true,
              nomsNumber = "G5555TT"
          ))
          .post("/match")
          .then()
          .statusCode(200)
          .body("matchedBy", equalTo("HMPPS_KEY"))
          .body("matches.findall.size()", equalTo(1))
          .body("matches[0].offender.otherIds.crn", equalTo("X00002"))
    }

    @Test
    internal fun `should not match using CRO number if no other data matches the record other then CRO number`() {
      given()
          .auth()
          .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .body(MatchRequest(
              surname = "ahankara",
              firstName = "adi",
              dateOfBirth = LocalDate.of(1977, 1, 6),
              croNumber = "SF80/655108T",
              activeSentence = true
          ))
          .post("/match")
          .then()
          .statusCode(200)
          .body("matches.findall.size()", equalTo(0))
    }

    @Test
    internal fun `should match using CRO number if CRO and surname match`() {
      given()
          .auth()
          .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .body(MatchRequest(
              surname = "gramsci",
              firstName = "june",
              dateOfBirth = LocalDate.of(1977, 1, 6),
              croNumber = "SF80/655108T",
              activeSentence = true
          ))
          .post("/match")
          .then()
          .statusCode(200)
          .body("matches.findall.size()", equalTo(1))
          .body("matches[0].offender.otherIds.crn", equalTo("X00001"))
    }

    @Test
    internal fun `should match using CRO number if CRO and date of birth match`() {
      given()
          .auth()
          .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .body(MatchRequest(
              surname = "ahankara",
              firstName = "adi",
              dateOfBirth = LocalDate.of(1988, 1, 6),
              croNumber = "SF80/655108T",
              activeSentence = true
          ))
          .post("/match")
          .then()
          .statusCode(200)
          .body("matches.findall.size()", equalTo(1))
          .body("matches[0].offender.otherIds.crn", equalTo("X00001"))
    }

    @Test
    internal fun `should match using CRO number if CRO and alias surname match`() {
      given()
          .auth()
          .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .body(MatchRequest(
              surname = "abbagnano",
              firstName = "june",
              dateOfBirth = LocalDate.of(1977, 1, 6),
              croNumber = "SF80/655108T",
              activeSentence = true
          ))
          .post("/match")
          .then()
          .statusCode(200)
          .body("matches.findall.size()", equalTo(1))
          .body("matches[0].offender.otherIds.crn", equalTo("X00001"))
    }

    @Test
    internal fun `should match using CRO number if CRO and alias date of birth match`() {
      given()
          .auth()
          .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .body(MatchRequest(
              surname = "ahankara",
              firstName = "adi",
              dateOfBirth = LocalDate.of(1990, 9, 1),
              croNumber = "SF80/655108T",
              activeSentence = true
          ))
          .post("/match")
          .then()
          .statusCode(200)
          .body("matches.findall.size()", equalTo(1))
          .body("matches[0].offender.otherIds.crn", equalTo("X00001"))
    }

  }

  @Nested
  inner class NameMatching {
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
    internal fun `should match using name and date of birth`() {
      given()
          .auth()
          .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .body(MatchRequest(
              surname = "adorno",
              firstName = "theodor",
              dateOfBirth = LocalDate.of(1903, 11, 11),
              croNumber = "SF80/655108T"
          ))
          .post("/match")
          .then()
          .statusCode(200)
          .body("matchedBy", equalTo("NAME"))
          .body("matches.findall.size()", equalTo(1))
          .body("matches[0].offender.otherIds.crn", equalTo("X00001"))
    }

    @Test
    internal fun `should match using any alias name and date of birth`() {
      given()
          .auth()
          .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .body(MatchRequest(
              surname = "Abbagnano",
              firstName = "Nicola",
              dateOfBirth = LocalDate.of(1990, 9, 1),
              croNumber = "SF80/655108T"
          ))
          .post("/match")
          .then()
          .statusCode(200)
          .body("matchedBy", equalTo("NAME"))
          .body("matches.findall.size()", equalTo(1))
          .body("matches[0].offender.otherIds.crn", equalTo("X00001"))

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
          .body("matches[0].offender.otherIds.crn", equalTo("X00001"))
    }

    @Test
    internal fun `should not cross match across aliases and primary names and date of births`() {
      given()
          .auth()
          .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .body(MatchRequest(
              surname = "Abbagnano",
              firstName = "Bhimrao",
              dateOfBirth = LocalDate.of(1903, 11, 11),
              croNumber = "SF80/655108T"
          ))
          .post("/match")
          .then()
          .statusCode(200)
          .body("matchedBy", equalTo("NOTHING"))
          .body("matches.findall.size()", equalTo(0))
    }

    @Test
    internal fun `should not anything when nothing matches`() {
      given()
          .auth()
          .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .body(MatchRequest(
              surname = "roger",
              firstName = "rabbit",
              dateOfBirth = LocalDate.of(2013, 12, 7),
              croNumber = "SF55/765108T"
          ))
          .post("/match")
          .then()
          .statusCode(200)
          .body("matches.findall.size()", equalTo(0))
          .body("matchedBy", equalTo("NOTHING"))
    }

  }

  @Nested
  inner class PartialNameMatching {
    @BeforeEach
    internal fun setup() {
      loadOffenders(
          OffenderIdentification(
              surname = "Adorno",
              firstName = "Theodor",
              dateOfBirth = LocalDate.of(1903, 11, 11),
              crn = "X00001"
          )
      )
    }

    @Test
    internal fun `should match using surname and date of birth`() {
      given()
          .auth()
          .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .body(MatchRequest(
              surname = "adorno",
              firstName = "bobby",
              dateOfBirth = LocalDate.of(1903, 11, 11),
              croNumber = "SF80/655108T"
          ))
          .post("/match")
          .then()
          .statusCode(200)
          .body("matchedBy", equalTo("PARTIAL_NAME"))
          .body("matches.findall.size()", equalTo(1))
          .body("matches[0].offender.otherIds.crn", equalTo("X00001"))
    }


    @Test
    internal fun `should not match when just surname matches`() {
      given()
          .auth()
          .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .body(MatchRequest(
              surname = "adorno",
              firstName = "rabbit",
              dateOfBirth = LocalDate.of(2013, 12, 7),
              croNumber = "SF55/765108T"
          ))
          .post("/match")
          .then()
          .statusCode(200)
          .body("matches.findall.size()", equalTo(0))
          .body("matchedBy", equalTo("NOTHING"))
    }
  }

  @Nested
  inner class PartialNameDOBLenientMatching {
    @BeforeEach
    internal fun setup() {
      loadOffenders(
          OffenderIdentification(
              surname = "Adorno",
              firstName = "Theodor",
              dateOfBirth = LocalDate.of(1903, 11, 7),
              crn = "X00001"
          )
      )
    }

    @Test
    internal fun `should match using surname and date of birth with swapped day month`() {
      given()
          .auth()
          .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .body(MatchRequest(
              surname = "adorno",
              firstName = "Theodor",
              dateOfBirth = LocalDate.of(1903, 7, 11),
              croNumber = "SF80/655108T"
          ))
          .post("/match")
          .then()
          .statusCode(200)
          .body("matchedBy", equalTo("PARTIAL_NAME_DOB_LENIENT"))
          .body("matches.findall.size()", equalTo(1))
          .body("matches[0].offender.otherIds.crn", equalTo("X00001"))
    }

    @Test
    internal fun `should match using surname and date of birth with different day`() {
      given()
          .auth()
          .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .body(MatchRequest(
              surname = "adorno",
              firstName = "Theodor",
              dateOfBirth = LocalDate.of(1903, 11, 8),
              croNumber = "SF80/655108T"
          ))
          .post("/match")
          .then()
          .statusCode(200)
          .body("matchedBy", equalTo("PARTIAL_NAME_DOB_LENIENT"))
          .body("matches.findall.size()", equalTo(1))
          .body("matches[0].offender.otherIds.crn", equalTo("X00001"))
    }


    @Test
    internal fun `should not match when just surname matches`() {
      given()
          .auth()
          .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .body(MatchRequest(
              surname = "adorno",
              firstName = "rabbit",
              dateOfBirth = LocalDate.of(2013, 12, 7),
              croNumber = "SF55/765108T"
          ))
          .post("/match")
          .then()
          .statusCode(200)
          .body("matches.findall.size()", equalTo(0))
          .body("matchedBy", equalTo("NOTHING"))
    }
  }
}

