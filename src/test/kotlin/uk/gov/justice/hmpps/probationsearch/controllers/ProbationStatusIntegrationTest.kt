package uk.gov.justice.hmpps.probationsearch.controllers

import io.restassured.RestAssured
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.hmpps.probationsearch.util.PersonSearchHelper

class ProbationStatusIntegrationTest : ElasticIntegrationBase() {

  @BeforeEach
  fun `load offenders`() {
    PersonSearchHelper(openSearchClient).loadData()
  }

  @Nested
  inner class OffenderSearch {
    @Test
    fun `ProbationStatus is returned from the search`() {
      val response = RestAssured.given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_PROBATION__SEARCH_PERSON"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("""{"crn": "X00001"}""")
        .post("/search")
        .then()
        .statusCode(200)
        .extract()
        .body().asString()

      assertThatJson(response).node("[0].otherIds.crn").isEqualTo("X00001")
      assertThatJson(response).node("[0].probationStatus.status").isEqualTo("CURRENT")
      assertThatJson(response).node("[0].probationStatus.inBreach").isEqualTo(true)
      assertThatJson(response).node("[0].probationStatus.preSentenceActivity").isEqualTo(false)
      assertThatJson(response).node("[0].probationStatus.awaitingPsr").isEqualTo(true)
      assertThatJson(response).node("[0].probationStatus.previouslyKnownTerminationDate").isEqualTo("2021-02-08")
    }

    @Test
    fun `ProbationStatus with null fields is handled in the response`() {
      val response = RestAssured.given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_PROBATION__SEARCH_PERSON"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("""{"crn": "X00004"}""")
        .post("/search")
        .then()
        .statusCode(200)
        .extract()
        .body().asString()

      assertThatJson(response).node("[0].otherIds.crn").isEqualTo("X00004")
      assertThatJson(response).node("[0].probationStatus.status").isEqualTo("PREVIOUSLY_KNOWN")
      assertThatJson(response).node("[0].probationStatus.inBreach").isAbsent()
      assertThatJson(response).node("[0].probationStatus.preSentenceActivity").isEqualTo(false)
      assertThatJson(response).node("[0].probationStatus.awaitingPsr").isEqualTo(true)
      assertThatJson(response).node("[0].probationStatus.previouslyKnownTerminationDate").isAbsent()
    }

    @Test
    fun `Missing ProbationStatus is not included in the response`() {
      val response = RestAssured.given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_PROBATION__SEARCH_PERSON"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("""{"crn": "X00002"}""")
        .post("/search")
        .then()
        .statusCode(200)
        .extract()
        .body().asString()

      assertThatJson(response).node("[0].otherIds.crn").isEqualTo("X00002")
      assertThatJson(response).node("[0].probationStatus").isAbsent()
    }
  }

  @Nested
  inner class OffenderMatch {
    @Test
    fun `ProbationStatus is returned from the match`() {
      val response = RestAssured.given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_PROBATION__SEARCH_PERSON"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("""{"surname": "Smith", "dateOfBirth": "1978-01-06"}""")
        .post("/match")
        .then()
        .statusCode(200)
        .extract()
        .body().asString()

      assertThatJson(response).node("matches[0].offender.otherIds.crn").isEqualTo("X00001")
      assertThatJson(response).node("matches[0].offender.probationStatus.status").isEqualTo("CURRENT")
      assertThatJson(response).node("matches[0].offender.probationStatus.inBreach").isEqualTo(true)
      assertThatJson(response).node("matches[0].offender.probationStatus.awaitingPsr").isEqualTo(true)
      assertThatJson(response).node("matches[0].offender.probationStatus.preSentenceActivity").isEqualTo(false)
      assertThatJson(response).node("matches[0].offender.probationStatus.previouslyKnownTerminationDate").isEqualTo("2021-02-08")
    }

    @Test
    fun `ProbationStatus with null fields is returned from the match`() {
      val response = RestAssured.given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_PROBATION__SEARCH_PERSON"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("""{"surname": "Gramsci", "dateOfBirth": "1988-01-06"}""")
        .post("/match")
        .then()
        .statusCode(200)
        .extract()
        .body().asString()

      assertThatJson(response).node("matches[0].offender.otherIds.crn").isEqualTo("X00004")
      assertThatJson(response).node("matches[0].offender.probationStatus.status").isEqualTo("PREVIOUSLY_KNOWN")
      assertThatJson(response).node("matches[0].offender.probationStatus.inBreach").isAbsent()
      assertThatJson(response).node("matches[0].offender.probationStatus.preSentenceActivity").isEqualTo(false)
      assertThatJson(response).node("matches[0].offender.probationStatus.awaitingPsr").isEqualTo(true)
      assertThatJson(response).node("matches[0].offender.probationStatus.previouslyKnownTerminationDate").isAbsent()
    }

    @Test
    fun `Missing ProbationStatus is not included in the response`() {
      val response = RestAssured.given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_PROBATION__SEARCH_PERSON"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("""{"surname": "Smith", "dateOfBirth": "1978-01-16"}""")
        .post("/match")
        .then()
        .statusCode(200)
        .extract()
        .body().asString()

      assertThatJson(response).node("matches[0].offender.otherIds.crn").isEqualTo("X00002")
      assertThatJson(response).node("matches[0].offender.probationStatus").isAbsent()
    }
  }
}
