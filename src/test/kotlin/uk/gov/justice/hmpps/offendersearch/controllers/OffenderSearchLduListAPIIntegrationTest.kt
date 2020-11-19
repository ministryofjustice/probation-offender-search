package uk.gov.justice.hmpps.offendersearch.controllers

import io.restassured.RestAssured
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.hmpps.offendersearch.dto.KeyValue
import uk.gov.justice.hmpps.offendersearch.dto.OffenderDetail

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
  fun ignoreNonActiveOffenderManagerResults() {
    val results = RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("""["N01ALL", "N02ALL"]""")
      .post("/ldu-codes")
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(Array<OffenderDetail>::class.java)

    Assertions.assertThat(results[0].offenderManagers?.get(0)?.team?.localDeliveryUnit?.code).contains("N01ALL")
    Assertions.assertThat(results[1].offenderManagers?.get(0)?.team?.localDeliveryUnit?.code).contains("N02ALL")
    Assertions.assertThat(results).hasSize(2)
  }

  @Test
  fun lduCodeListSearch() {
    val results = RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("""["N01ALL", "N04ALL"]""")
      .post("/ldu-codes")
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(Array<OffenderDetail>::class.java)

    Assertions.assertThat(results).hasSize(1)
    Assertions.assertThat(results[0].offenderManagers?.get(0)?.team?.localDeliveryUnit?.code).contains("N01ALL")
  }

  @Test
  fun lduCodeListSearch_ignoreNotFoundIds() {
    val results = RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("""["N01ALL", "N02ALL", "DDD123"]""")
      .post("/ldu-codes")
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(Array<OffenderDetail>::class.java)
    Assertions.assertThat(results).hasSize(2)
    Assertions.assertThat(results[0].offenderManagers?.get(0)?.team?.localDeliveryUnit?.code).contains("N01ALL")
    Assertions.assertThat(results[1].offenderManagers?.get(0)?.team?.localDeliveryUnit?.code).contains("N02ALL")
  }

  @Test
  fun shouldFilterOutSoftDeletedOffenderRecords() {
    val results = RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("""["N03ALL"]""")
      .post("/ldu-codes")
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
      .body("""["N05ALL"]""")
      .post("/ldu-codes")
      .then()
      .statusCode(200)
      .extract()
      .body()
      .`as`(Array<OffenderDetail>::class.java)
    Assertions.assertThat(results).hasSize(0)
  }

  @Test
  fun noLduCodesList_badRequest() {
    RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .post("/ldu-codes")
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
      .body("""["AAA123", "BBB123"]""")
      .post("/ldu-codes")
      .then()
      .statusCode(200)
      .extract()
      .`as`(Array<OffenderDetail>::class.java)
    Assertions.assertThat(results).hasSize(0)
  }

  @Test
  fun `will limit result of the supplied page for the supplied size`() {
    loadBulkUsers(20)
    val results = RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .queryParam("page", 0)
      .queryParam("size", 12)
      .body("""["N09ALL"]""")
      .post("/ldu-codes")
      .then()
      .statusCode(200)
      .extract()
      .`as`(Array<OffenderDetail>::class.java)
    Assertions.assertThat(results).hasSize(12)
  }

  @Test
  fun `will give results of the default page and for default size`() {
    loadBulkUsers(20)
    val results = RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .body("""["N09ALL"]""")
      .post("/ldu-codes")
      .then()
      .statusCode(200)
      .extract()
      .`as`(Array<OffenderDetail>::class.java)
    Assertions.assertThat(results).hasSize(10)
  }

  @Test
  fun `will give results of next page`() {
    loadBulkUsers()
    val results = RestAssured.given()
      .auth()
      .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .queryParam("page", 1)
      .body("""["N09ALL"]""")
      .post("/ldu-codes")
      .then()
      .statusCode(200)
      .extract()
      .`as`(Array<OffenderDetail>::class.java)
    Assertions.assertThat(results).hasSize(5)
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
