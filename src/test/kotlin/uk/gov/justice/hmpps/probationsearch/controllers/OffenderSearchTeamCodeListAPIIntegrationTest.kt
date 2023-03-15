package uk.gov.justice.hmpps.probationsearch.controllers

import io.restassured.RestAssured
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType

class OffenderSearchTeamCodeListAPIIntegrationTest : ElasticIntegrationBase() {

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
              code = "N02000",
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
      OffenderReplacement(
        offenderManagers = listOf(
          OffenderManagerReplacement(
            team = TeamReplacement(
              code = "N01000",
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
      .body("""["N01000","N03000"]""")
      .post("/team-codes")
      .then()
      .statusCode(403)
  }

  @Test
  fun `search for offenders by team code`() {
    RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .queryParam("page", 0)
      .body("""["N01000"]""")
      .post("/team-codes")
      .then()
      .statusCode(200)
      .body("content.size()", equalTo(2))
      .body("content[0].offenderManagers?.get(0)?.team?.code", equalTo("N01000"))
      .body("content[1].offenderManagers?.get(0)?.team?.code", equalTo("N01000"))
  }

  @Test
  fun `should ignore non-active offender manager results`() {
    RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .queryParam("page", 0)
      .body("""["N01000","N03000"]""")
      .post("/team-codes")
      .then()
      .statusCode(200)
      .body("content.size()", equalTo(2))
      .body("content[0].offenderManagers?.get(0)?.team?.code", equalTo("N01000"))
      .body("content[1].offenderManagers?.get(0)?.team?.code", equalTo("N01000"))
  }

  @Test
  fun `should ignore id's that are not found`() {
    RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .queryParam("page", 0)
      .body("""["N01000", "N02000", "AAAA123"]""")
      .post("/team-codes")
      .then()
      .statusCode(200)
      .body("content.size()", equalTo(3))
      .body("content[0].offenderManagers?.get(0)?.team?.code", equalTo("N02000"))
      .body("content[1].offenderManagers?.get(0)?.team?.code", equalTo("N01000"))
      .body("content[2].offenderManagers?.get(0)?.team?.code", equalTo("N01000"))
  }

  @Test
  fun `should filter out offenders that are soft deleted`() {
    RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .queryParam("page", 0)
      .body("""["N04000"]""")
      .post("/team-codes")
      .then()
      .statusCode(200)
      .body("content.size()", equalTo(0))
  }

  @Test
  fun `should filter out offenders that have a manager that is soft deleted`() {
    RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .queryParam("page", 0)
      .body("""["N05000"]""")
      .post("/team-codes")
      .then()
      .statusCode(200)
      .body("content.size()", equalTo(0))
  }

  @Test
  fun `by default will return a page of 10 offenders along with totals`() {
    loadBulkUsers()
    RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("""["N09000"]""")
      .post("/team-codes")
      .then()
      .statusCode(200)
      .body("content.size()", equalTo(10))
      .body("size", equalTo(10))
      .body("numberOfElements", equalTo(10))
      .body("totalElements", equalTo(12))
      .body("totalPages", equalTo(2))
      .body("pageable.offset", equalTo(0))
      .body("pageable.paged", equalTo(true))
      .body("pageable.pageSize", equalTo(10))
  }

  @Test
  fun `can specify page`() {
    loadBulkUsers()
    RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .queryParam("page", 1)
      .body("""["N09000"]""")
      .post("/team-codes")
      .then()
      .statusCode(200)
      .body("content.size()", equalTo(2))
      .body("totalElements", equalTo(12))
      .body("totalPages", equalTo(2))
      .body("pageable.offset", equalTo(10))
  }

  @Test
  fun `can specify page size`() {
    loadBulkUsers()
    RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .queryParam("size", 5)
      .body("""["N09000"]""")
      .post("/team-codes")
      .then()
      .statusCode(200)
      .body("content.size()", equalTo(5))
      .body("totalElements", equalTo(12))
      .body("totalPages", equalTo(3))
      .body("pageable.offset", equalTo(0))
  }

  @Test
  fun `a bad request should return a 400 status code`() {
    RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .queryParam("page", 0)
      .post("/team-codes")
      .then()
      .statusCode(400)
  }

  @Test
  fun `a request body containing an empty list should return a 400 status code`() {
    RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("""[]""")
      .post("/team-codes")
      .then()
      .statusCode(400)
  }

  @Test
  fun `no results should be returned if there are no matching id's`() {
    RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .queryParam("page", 0)
      .body("""["AAA123", "BBB123"]""")
      .post("/team-codes")
      .then()
      .statusCode(200)
      .body("content.size()", equalTo(0))
  }
}
