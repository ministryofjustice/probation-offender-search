package uk.gov.justice.hmpps.offendersearch.controllers

import io.restassured.RestAssured
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.hmpps.offendersearch.dto.KeyValue

class OffenderSearchLduListAPIIntegrationTest : LocalstackIntegrationBase() {

  @BeforeEach
  fun setUp() {
    loadOffenders(
      OffenderReplacement(
        offenderManagers = listOf(
          OffenderManagerReplacement(
            team = TeamReplacement(
              localDeliveryUnit = KeyValue(code = "N01ALL")
            )
          )
        )
      ),
      OffenderReplacement(
        offenderManagers = listOf(
          OffenderManagerReplacement(
            team = TeamReplacement(
              localDeliveryUnit = KeyValue(code = "N02ALL")
            )
          )
        )
      ),
      OffenderReplacement(
        deleted = true,
        offenderManagers = listOf(
          OffenderManagerReplacement(
            team = TeamReplacement(
              localDeliveryUnit = KeyValue(code = "N03ALL")
            )
          )
        )
      ),
      OffenderReplacement(
        offenderManagers = listOf(
          OffenderManagerReplacement(
            active = false,
            team = TeamReplacement(
              localDeliveryUnit = KeyValue(code = "N04ALL")
            )
          )
        )
      ),
      OffenderReplacement(
        offenderManagers = listOf(
          OffenderManagerReplacement(
            softDeleted = true,
            team = TeamReplacement(
              localDeliveryUnit = KeyValue(code = "N05ALL")
            )
          )
        )
      )
    )
  }

  @Test
  fun `not allowed to do a search without COMMUNITY role`() {
    RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_BINGO"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("""["N01ALL","N02ALL"]""")
      .post("/ldu-codes")
      .then()
      .statusCode(403)
  }

  @Test
  fun `should ignore non-active offender manager results`() {
    RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("""["N01ALL", "N02ALL"]""")
      .post("/ldu-codes")
      .then()
      .statusCode(200)
      .body("content.size()", equalTo(2))
      .body("content[0].offenderManagers?.get(0)?.team?.localDeliveryUnit?.code", equalTo("N01ALL"))
      .body("content[1].offenderManagers?.get(0)?.team?.localDeliveryUnit?.code", equalTo("N02ALL"))
  }

  @Test
  fun `search for offenders by ldu code`() {
    RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("""["N01ALL", "N04ALL"]""")
      .post("/ldu-codes")
      .then()
      .statusCode(200)
      .body("content.size()", equalTo(1))
      .body("content[0].offenderManagers?.get(0)?.team?.localDeliveryUnit?.code", equalTo("N01ALL"))
  }

  @Test
  fun `should ignore id's that are not found`() {
    RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("""["N01ALL", "N02ALL", "DDD123"]""")
      .post("/ldu-codes")
      .then()
      .statusCode(200)
      .body("content.size()", equalTo(2))
      .body("content[0].offenderManagers?.get(0)?.team?.localDeliveryUnit?.code", equalTo("N01ALL"))
      .body("content[1].offenderManagers?.get(0)?.team?.localDeliveryUnit?.code", equalTo("N02ALL"))
  }

  @Test
  fun `should filter out offenders that are soft deleted`() {
    RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("""["N03ALL"]""")
      .post("/ldu-codes")
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
      .body("""["N05ALL"]""")
      .post("/ldu-codes")
      .then()
      .statusCode(200)
      .body("content.size()", equalTo(0))
  }

  @Test
  fun `a bad request should return a 400 status code`() {
    RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .post("/ldu-codes")
      .then()
      .statusCode(400)
  }

  @Test
  fun `no results should be returned if there are no matching id's`() {
    RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("""["AAA123", "BBB123"]""")
      .post("/ldu-codes")
      .then()
      .statusCode(200)
      .body("content.size()", equalTo(0))
  }

  @Test
  fun `by default will return a page of 10 offenders along with totals`() {
    loadBulkUsers(20)
    RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("""["N09ALL"]""")
      .post("/ldu-codes")
      .then()
      .statusCode(200)
      .body("content.size()", equalTo(10))
      .body("size", equalTo(10))
      .body("numberOfElements", equalTo(10))
      .body("totalElements", equalTo(20))
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
      .body("""["N09ALL"]""")
      .post("/ldu-codes")
      .then()
      .statusCode(200)
      .body("content.size()", equalTo(5))
      .body("totalElements", equalTo(15))
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
      .body("""["N09ALL"]""")
      .post("/ldu-codes")
      .then()
      .statusCode(200)
      .body("content.size()", equalTo(5))
      .body("totalElements", equalTo(15))
      .body("totalPages", equalTo(3))
      .body("pageable.offset", equalTo(0))
  }

  private fun loadBulkUsers(count: Int = 15) {
    var offendersToLoad = (0 until count).map {
      OffenderReplacement(
        offenderManagers = listOf(
          OffenderManagerReplacement(
            team = TeamReplacement(localDeliveryUnit = KeyValue(code = "N09ALL"))
          )
        )
      )
    }.toTypedArray()
    loadOffenders(*offendersToLoad)
  }
}
