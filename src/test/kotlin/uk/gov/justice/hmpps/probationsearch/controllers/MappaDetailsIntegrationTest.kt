package uk.gov.justice.hmpps.probationsearch.controllers

import io.restassured.RestAssured
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.hmpps.probationsearch.util.PersonSearchHelper

class MappaDetailsIntegrationTest : ElasticIntegrationBase() {

  @BeforeEach
  fun `load offenders`() {
    PersonSearchHelper(esClient).loadData()
  }

  @Nested
  inner class OffenderSearch {
    @Test
    fun `MAPPA details are returned from the search`() {
      val response = RestAssured.given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("""{"crn": "X00001"}""")
        .post("/search")
        .then()
        .statusCode(200)
        .extract()
        .body().asString()

      assertThatJson(response).node("[0].otherIds.crn").isEqualTo("X00001")
      assertThatJson(response).node("[0].mappa.level").isEqualTo(1)
      assertThatJson(response).node("[0].mappa.levelDescription").isEqualTo("MAPPA Level 1")
      assertThatJson(response).node("[0].mappa.category").isEqualTo(2)
      assertThatJson(response).node("[0].mappa.categoryDescription").isEqualTo("MAPPA Category 2")
      assertThatJson(response).node("[0].mappa.startDate").isEqualTo("2021-02-08")
      assertThatJson(response).node("[0].mappa.reviewDate").isEqualTo("2021-05-08")
      assertThatJson(response).node("[0].mappa.team.code").isEqualTo("NO2AAM")
      assertThatJson(response).node("[0].mappa.team.description").isEqualTo("OMIC OMU A")
      assertThatJson(response).node("[0].mappa.officer.code").isEqualTo("NO2AAMU")
      assertThatJson(response).node("[0].mappa.officer.forenames").isEqualTo("Unallocated")
      assertThatJson(response).node("[0].mappa.officer.surname").isEqualTo("Staff")
      assertThatJson(response).node("[0].mappa.probationArea.code").isEqualTo("NO2")
      assertThatJson(response).node("[0].mappa.probationArea.description").isEqualTo("NPS London")
      assertThatJson(response).node("[0].mappa.notes").isEqualTo("Level 1 Cat 2 notes")
    }

    @Test
    fun `Missing MAPPA details are not included in the response`() {
      val response = RestAssured.given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("""{"crn": "X00002"}""")
        .post("/search")
        .then()
        .statusCode(200)
        .extract()
        .body().asString()

      assertThatJson(response).node("[0].otherIds.crn").isEqualTo("X00002")
      assertThatJson(response).node("[0].mappa").isAbsent()
    }
  }

  @Nested
  inner class OffenderMatch {
    @Test
    fun `MAPPA details are returned from the search`() {
      val response = RestAssured.given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("""{"surname": "Smith", "dateOfBirth": "1978-01-06"}""")
        .post("/match")
        .then()
        .statusCode(200)
        .extract()
        .body().asString()

      assertThatJson(response).node("matches[0].offender.otherIds.crn").isEqualTo("X00001")
      assertThatJson(response).node("matches[0].offender.mappa.level").isEqualTo(1)
      assertThatJson(response).node("matches[0].offender.mappa.levelDescription").isEqualTo("MAPPA Level 1")
      assertThatJson(response).node("matches[0].offender.mappa.category").isEqualTo(2)
      assertThatJson(response).node("matches[0].offender.mappa.categoryDescription").isEqualTo("MAPPA Category 2")
      assertThatJson(response).node("matches[0].offender.mappa.startDate").isEqualTo("2021-02-08")
      assertThatJson(response).node("matches[0].offender.mappa.reviewDate").isEqualTo("2021-05-08")
      assertThatJson(response).node("matches[0].offender.mappa.team.code").isEqualTo("NO2AAM")
      assertThatJson(response).node("matches[0].offender.mappa.team.description").isEqualTo("OMIC OMU A")
      assertThatJson(response).node("matches[0].offender.mappa.officer.code").isEqualTo("NO2AAMU")
      assertThatJson(response).node("matches[0].offender.mappa.officer.forenames").isEqualTo("Unallocated")
      assertThatJson(response).node("matches[0].offender.mappa.officer.surname").isEqualTo("Staff")
      assertThatJson(response).node("matches[0].offender.mappa.probationArea.code").isEqualTo("NO2")
      assertThatJson(response).node("matches[0].offender.mappa.probationArea.description").isEqualTo("NPS London")
      assertThatJson(response).node("matches[0].offender.mappa.notes").isEqualTo("Level 1 Cat 2 notes")
    }

    @Test
    fun `Missing MAPPA details are not included in the response`() {
      val response = RestAssured.given()
        .auth()
        .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("""{"surname": "Smith", "dateOfBirth": "1978-01-16"}""")
        .post("/match")
        .then()
        .statusCode(200)
        .extract()
        .body().asString()

      assertThatJson(response).node("matches[0].offender.otherIds.crn").isEqualTo("X00002")
      assertThatJson(response).node("matches[0].offender.mappa").isAbsent()
    }
  }
}
