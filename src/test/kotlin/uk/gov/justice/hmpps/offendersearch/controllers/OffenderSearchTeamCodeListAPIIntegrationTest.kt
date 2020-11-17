package uk.gov.justice.hmpps.offendersearch.controllers

import io.restassured.RestAssured
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.hmpps.offendersearch.dto.OffenderDetail

class OffenderSearchTeamCodeListAPIIntegrationTest : LocalstackIntegrationBase() {

  @BeforeEach
  fun setUp() {
    loadOffenders(
      OffenderReplacement(
        offenderManagers = listOf(
          OffenderManagerReplacement(
            team = TeamReplacement(
              code = "N01000"
            )
          )
        )
      ),
      OffenderReplacement(
        offenderManagers = listOf(
          OffenderManagerReplacement(
            team = TeamReplacement(
              code = "N02000"
            )
          )
        )
      ),
      OffenderReplacement(
        offenderManagers = listOf(
          OffenderManagerReplacement(
            active = false,
            team = TeamReplacement(
              code = "N03000"
            )
          )
        )
      ),
      OffenderReplacement(
        deleted = true,
        offenderManagers = listOf(
          OffenderManagerReplacement(
            team = TeamReplacement(
              code = "N04000"
            )
          )
        )
      ),
      OffenderReplacement(
        offenderManagers = listOf(
          OffenderManagerReplacement(
            softDeleted = true,
            team = TeamReplacement(
              code = "N05000"
            )
          )
        )
      ),
      OffenderReplacement(
        offenderManagers = listOf(
          OffenderManagerReplacement(
            team = TeamReplacement(
              code = "N01000"
            )
          )
        )
      )
    )
  }

  fun loadBulkUsers() {
    val offendersToLoad = (0..11).map {
      OffenderReplacement(
        offenderManagers = listOf(
          OffenderManagerReplacement(
            team = TeamReplacement()
          )
        )
      )
    }.toTypedArray()
    loadOffenders(*offendersToLoad)
  }

  @Test
  fun `not allowed to do a search without COMMUNITY role`() {
    RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_BINGO"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("""["N01000","N03000"]""")
      .post("/team-codes")
      .then()
      .statusCode(403)
  }

  @Test
  fun teamCodeListSearch() {
    val results = RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .queryParam("page", 0)
      .body("""["N01000"]""")
      .post("/team-codes")
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(Array<OffenderDetail>::class.java)

    Assertions.assertThat(results).hasSize(2)
    Assertions.assertThat(results[0].offenderManagers?.get(0)?.team?.code).contains("N01000")
    Assertions.assertThat(results[1].offenderManagers?.get(0)?.team?.code).contains("N01000")
  }

  @Test
  fun ignoreNonActiveOffenderManagerResults() {
    val results = RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .queryParam("page", 0)
      .body("""["N01000","N03000"]""")
      .post("/team-codes")
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(Array<OffenderDetail>::class.java)

    Assertions.assertThat(results).hasSize(2)
    Assertions.assertThat(results[0].offenderManagers?.get(0)?.team?.code).contains("N01000")
    Assertions.assertThat(results[1].offenderManagers?.get(0)?.team?.code).contains("N01000")
  }

  @Test
  fun teamCodeListSearch_ignoreNotFoundIds() {
    val results = RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .queryParam("page", 0)
      .body("""["N01000", "N02000", "AAAA123"]""")
      .post("/team-codes")
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(Array<OffenderDetail>::class.java)

    Assertions.assertThat(results).hasSize(3)
    Assertions.assertThat(results[0].offenderManagers?.get(0)?.team?.code).contains("N02000")
    Assertions.assertThat(results[1].offenderManagers?.get(0)?.team?.code).contains("N01000")
    Assertions.assertThat(results[2].offenderManagers?.get(0)?.team?.code).contains("N01000")
  }

  @Test
  fun shouldFilterOutSoftDeletedOffenderRecords() {
    val results = RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .queryParam("page", 0)
      .body("""["N04000"]""")
      .post("/team-codes")
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(Array<OffenderDetail>::class.java)

    Assertions.assertThat(results).hasSize(0)
  }

  @Test
  fun shouldFilterOutSoftDeletedOffenderManagerRecords() {
    val results = RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .queryParam("page", 0)
      .body("""["N05000"]""")
      .post("/team-codes")
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(Array<OffenderDetail>::class.java)
    Assertions.assertThat(results).hasSize(0)
  }

  @Test
  fun limitResultsToPageSize() {
    loadBulkUsers()
    val results = RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .queryParam("page", 0)
      .body("""["N09000"]""")
      .post("/team-codes")
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(Array<OffenderDetail>::class.java)

    Assertions.assertThat(results).hasSize(10)
  }

  @Test
  fun checkResultsGoToNextPage() {
    loadBulkUsers()
    val results = RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .queryParam("page", 1)
      .body("""["N09000"]""")
      .post("/team-codes")
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(Array<OffenderDetail>::class.java)

    Assertions.assertThat(results).hasSize(2)
  }

  @Test
  fun noTeamCodesList_badRequest() {
    RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .queryParam("page", 0)
      .post("/team-codes")
      .then()
      .statusCode(400)
      .extract()
      .body()
  }

  @Test
  fun noResults() {
    val results = RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .queryParam("page", 0)
      .body("""["AAA123", "BBB123"]""")
      .post("/team-codes")
      .then()
      .statusCode(200)
      .extract()
      .`as`(Array<OffenderDetail>::class.java)
    Assertions.assertThat(results).hasSize(0)
  }
}
