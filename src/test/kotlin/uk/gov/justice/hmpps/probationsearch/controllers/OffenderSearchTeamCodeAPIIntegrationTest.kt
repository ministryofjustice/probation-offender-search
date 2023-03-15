package uk.gov.justice.hmpps.probationsearch.controllers

import io.restassured.RestAssured
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType

class OffenderSearchTeamCodeAPIIntegrationTest : ElasticIntegrationBase() {

  @BeforeEach
  fun setUp() {
    loadOffenders(
      OffenderReplacement(
        offenderManagers = listOf(
          OffenderManagerReplacement(
            team = TeamReplacement(
              code = "N01000",
            ),
          ),
        ),
      ),
      OffenderReplacement(
        offenderManagers = listOf(
          OffenderManagerReplacement(
            team = TeamReplacement(
              code = "N01000",
            ),
          ),
        ),
      ),
      OffenderReplacement(
        offenderManagers = listOf(
          OffenderManagerReplacement(
            active = false,
            team = TeamReplacement(
              code = "N03000",
            ),
          ),
        ),
      ),
      OffenderReplacement(
        deleted = true,
        offenderManagers = listOf(
          OffenderManagerReplacement(
            team = TeamReplacement(
              code = "N04000",
            ),
          ),
        ),
      ),
      OffenderReplacement(
        offenderManagers = listOf(
          OffenderManagerReplacement(
            softDeleted = true,
            team = TeamReplacement(
              code = "N05000",
            ),
          ),
        ),
      ),
    )
  }

  fun loadBulkUsers() {
    val offendersToLoad = (0..11).map {
      OffenderReplacement(
        offenderManagers = listOf(
          OffenderManagerReplacement(
            team = TeamReplacement(),
          ),
        ),
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
      .get("/team-codes/N01000")
      .then()
      .statusCode(403)
  }

  @Test
  fun `search for offenders by team code`() {
    RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .get("/team-codes/N01000")
      .then()
      .statusCode(200)
      .body("content.size()", Matchers.equalTo(2))
      .body("content[0].offenderManagers?.get(0)?.team?.code", Matchers.equalTo("N01000"))
      .body("content[1].offenderManagers?.get(0)?.team?.code", Matchers.equalTo("N01000"))
  }

  @Test
  fun `should ignore non-active offender manager results`() {
    RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .get("/team-codes/N03000")
      .then()
      .statusCode(200)
      .body("content.size()", Matchers.equalTo(0))
  }

  @Test
  fun `should filter out offenders that are soft deleted`() {
    RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .get("/team-codes/N04000")
      .then()
      .statusCode(200)
      .body("content.size()", Matchers.equalTo(0))
  }

  @Test
  fun `should filter out offenders that have a manager that is soft deleted`() {
    RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .get("/team-codes/N05000")
      .then()
      .statusCode(200)
      .body("content.size()", Matchers.equalTo(0))
  }

  @Test
  fun `by default will return a page of 10 offenders along with totals`() {
    loadBulkUsers()
    RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .get("/team-codes/N09000")
      .then()
      .statusCode(200)
      .body("content.size()", Matchers.equalTo(10))
      .body("size", Matchers.equalTo(10))
      .body("numberOfElements", Matchers.equalTo(10))
      .body("totalElements", Matchers.equalTo(12))
      .body("totalPages", Matchers.equalTo(2))
      .body("pageable.offset", Matchers.equalTo(0))
      .body("pageable.paged", Matchers.equalTo(true))
      .body("pageable.pageSize", Matchers.equalTo(10))
  }

  @Test
  fun `can specify page`() {
    loadBulkUsers()
    RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .queryParam("page", 1)
      .get("/team-codes/N09000")
      .then()
      .statusCode(200)
      .body("content.size()", Matchers.equalTo(2))
      .body("totalElements", Matchers.equalTo(12))
      .body("totalPages", Matchers.equalTo(2))
      .body("pageable.offset", Matchers.equalTo(10))
  }

  @Test
  fun `can specify page size`() {
    loadBulkUsers()
    RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .queryParam("size", 5)
      .get("/team-codes/N09000")
      .then()
      .statusCode(200)
      .body("content.size()", Matchers.equalTo(5))
      .body("totalElements", Matchers.equalTo(12))
      .body("totalPages", Matchers.equalTo(3))
      .body("pageable.offset", Matchers.equalTo(0))
  }

  @Test
  fun `no results should be returned if there are no matching id's`() {
    RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .get("/team-codes/ABC123")
      .then()
      .statusCode(200)
      .body("content.size()", Matchers.equalTo(0))
  }
}
