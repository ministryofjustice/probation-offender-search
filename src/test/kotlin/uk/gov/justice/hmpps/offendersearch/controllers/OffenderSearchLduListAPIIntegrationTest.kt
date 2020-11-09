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
                                                localDeliveryUnit = KeyValue(code = "AAA123")
                                        )
                                )
                        )
                ),
                OffenderReplacement(
                        offenderManagers = listOf(
                                OffenderManagerReplacement(
                                        team = TeamReplacement(
                                                localDeliveryUnit = KeyValue(code = "BBB123")
                                        )
                                )
                        )
                ),
                OffenderReplacement(
                        deleted = true,
                        offenderManagers = listOf(
                                OffenderManagerReplacement(
                                        team = TeamReplacement(
                                                localDeliveryUnit = KeyValue(code = "CCC123")
                                        )
                                )
                        )
                ),
                OffenderReplacement(
                        offenderManagers = listOf(
                                OffenderManagerReplacement(
                                        active = false,
                                        team = TeamReplacement(
                                                localDeliveryUnit = KeyValue(code = "HHH123")
                                        )
                                )
                        )
                ),
                OffenderReplacement(
                        offenderManagers = listOf(
                                OffenderManagerReplacement(
                                        softDeleted = true,
                                        team = TeamReplacement(
                                                localDeliveryUnit = KeyValue(code = "III123")
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
                .body("""["AAA123","BBB123"]""")
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
                .body("""["AAA123", "BBB123"]""")
                .post("/ldu-codes")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .`as`(Array<OffenderDetail>::class.java)

        Assertions.assertThat(results[0].offenderManagers?.get(0)?.team?.localDeliveryUnit?.code).contains("AAA123")
        Assertions.assertThat(results[1].offenderManagers?.get(0)?.team?.localDeliveryUnit?.code).contains("BBB123")
        Assertions.assertThat(results).hasSize(2)
    }


    @Test
    fun lduCodeListSearch() {
        val results = RestAssured.given()
                .auth()
                .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("""["AAA123", "HHH123"]""")
                .post("/ldu-codes")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .`as`(Array<OffenderDetail>::class.java)

        Assertions.assertThat(results).hasSize(1)
        Assertions.assertThat(results[0].offenderManagers?.get(0)?.team?.localDeliveryUnit?.code).contains("AAA123")
    }

    @Test
    fun lduCodeListSearch_ignoreNotFoundIds() {
        val results = RestAssured.given()
                .auth()
                .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("""["AAA123", "BBB123", "DDD123"]""")
                .post("/ldu-codes")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .`as`(Array<OffenderDetail>::class.java)
        Assertions.assertThat(results).hasSize(2)
        Assertions.assertThat(results[0].offenderManagers?.get(0)?.team?.localDeliveryUnit?.code).contains("AAA123")
        Assertions.assertThat(results[1].offenderManagers?.get(0)?.team?.localDeliveryUnit?.code).contains("BBB123")
    }

    @Test
    fun shouldFilterOutSoftDeletedOffenderRecords() {
        val results = RestAssured.given()
                .auth()
                .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("""["CCC123"]""")
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
                .body("""["III123"]""")
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
                .body("""["DDD123", "EEE123"]""")
                .post("/ldu-codes")
                .then()
                .statusCode(200)
                .extract()
                .`as`(Array<OffenderDetail>::class.java)
        Assertions.assertThat(results).hasSize(0)
    }

}