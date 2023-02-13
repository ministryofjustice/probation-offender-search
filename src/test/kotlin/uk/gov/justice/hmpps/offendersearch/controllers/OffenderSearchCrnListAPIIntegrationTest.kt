package uk.gov.justice.hmpps.offendersearch.controllers

import io.restassured.RestAssured.given
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.hmpps.offendersearch.dto.OffenderDetail

class OffenderSearchCrnListAPIIntegrationTest : LocalstackIntegrationBase() {

  @BeforeEach
  fun setUp() {
    loadOffenders(
      OffenderReplacement(crn = "X00001", previousCrn = "X10001"),
      OffenderReplacement(crn = "X00003"),
      OffenderReplacement(crn = "X00088", deleted = true),
    )
  }

  @Test
  fun `not allowed to do a search without COMMUNITY role`() {
    given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_BINGO"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("[\"X00001\",\"X00002\"]")
      .post("/crns")
      .then()
      .statusCode(403)
  }

  @Test
  fun crnListSearch() {
    val results = given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("[\"X00001\",\"X00003\"]")
      .post("/crns")
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(Array<OffenderDetail>::class.java)
    assertThat(results).hasSize(2)
    assertThat(results).extracting("otherIds.crn").containsExactly("X00001", "X00003")
  }

  @Test
  fun previousCrnSearch() {
    val results = given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("[\"X10001\"]")
      .post("/crns")
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(Array<OffenderDetail>::class.java)
    assertThat(results).hasSize(1)
    assertThat(results).extracting("otherIds.crn").containsExactly("X00001")
  }

  @Test
  fun crnListSearch_biggerPayload() {
    val offendersToLoad = (200..501).map {
      OffenderReplacement(crn = it.toCrn())
    }.toTypedArray()
    val offendersToSearch = (200..301).map {
      it.toCrn()
    }.toTypedArray()
    loadOffenders(*offendersToLoad)
    val results = given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body(offendersToSearch)
      .post("/crns")
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(Array<OffenderDetail>::class.java)
    assertThat(results).hasSize(102)
    assertThat(results).extracting("otherIds.crn").contains("X00200", "X00301")
  }

  @Test
  fun crnListSearch_ignoreNotFoundIds() {
    val results = given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("[\"X00001\",\"X00002\"]")
      .post("/crns")
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(Array<OffenderDetail>::class.java)
    assertThat(results).hasSize(1)
    assertThat(results).extracting("otherIds.crn").containsExactly("X00001")
  }

  @Test
  fun shouldFilterOutSoftDeletedRecords() {
    val results = given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("[\"X00088\"]")
      .post("/crns")
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(Array<OffenderDetail>::class.java)
    assertThat(results).hasSize(0)
  }

  @Test
  fun noCrnList_badRequest() {
    given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .post("/crns")
      .then()
      .statusCode(400)
      .extract()
      .body()
  }

  @Test
  fun noResults() {
    val results = given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("[\"AB1\",\"AB2\"]")
      .post("/crns")
      .then()
      .statusCode(200)
      .extract()
      .`as`(Array<OffenderDetail>::class.java)
    assertThat(results).hasSize(0)
  }
}
