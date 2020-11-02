package uk.gov.justice.hmpps.offendersearch.controllers

import io.restassured.RestAssured
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.hmpps.offendersearch.dto.OffenderDetail

class OffenderSearchNomsListAPIIntegrationTest : LocalstackIntegrationBase() {

    @BeforeEach
    fun setUp() {
        loadOffenders(
                OffenderReplacement(nomsNumber = "G8020GG"),
                OffenderReplacement(nomsNumber = "G8020GH"),
                OffenderReplacement(nomsNumber = "G8020GI", deleted = true),
        )
    }

    @Test
    fun `not allowed to do a search without COMMUNITY role`() {
        RestAssured.given()
                .auth()
                .oauth2(jwtAuthenticationHelper.createJwt("ROLE_BINGO"))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("[\"X00001\",\"X00002\"]")
                .post("/nomsNumbers")
                .then()
                .statusCode(403)
    }

    @Test
    fun nomsNumberListSearch() {
        val results = RestAssured.given()
                .auth()
                .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("[\"G8020GG\",\"G8020GH\"]")
                .post("/nomsNumbers")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .`as`(Array<OffenderDetail>::class.java)
        Assertions.assertThat(results).hasSize(2)
        Assertions.assertThat(results).extracting("otherIds.nomsNumber").containsExactly("G8020GG", "G8020GH")
    }

    @Test
    fun nomsNumberListSearch_biggerPayload() {
        val offendersToLoad = (200..501).map {
            OffenderReplacement(nomsNumber = it.toNomsNumber())
        }.toTypedArray()
        val offendersToSearch = (200..301).map {
            it.toNomsNumber()
        }.toTypedArray()
        loadOffenders(*offendersToLoad)
        val results = RestAssured.given()
                .auth()
                .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(offendersToSearch)
                .post("/nomsNumbers")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .`as`(Array<OffenderDetail>::class.java)
        Assertions.assertThat(results).hasSize(102)
        Assertions.assertThat(results).extracting("otherIds.nomsNumber").contains("G2000GG", "G2010GG")
    }

    @Test
    fun nomsNumberListSearch_ignoreNotFoundIds() {
        val results = RestAssured.given()
                .auth()
                .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("[\"G8020GG\",\"G7010GG\"]")
                .post("/nomsNumbers")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .`as`(Array<OffenderDetail>::class.java)
        Assertions.assertThat(results).hasSize(1)
        Assertions.assertThat(results).extracting("otherIds.nomsNumber").containsExactly("G8020GG")
    }

    @Test
    fun shouldFilterOutSoftDeletedRecords() {
        val results = RestAssured.given()
                .auth()
                .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("[\"G8020GI\"]")
                .post("/nomsNumbers")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .`as`(Array<OffenderDetail>::class.java)
        Assertions.assertThat(results).hasSize(0)
    }

    @Test
    fun noNomsNumberList_badRequest() {
        RestAssured.given()
                .auth()
                .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .post("/nomsNumbers")
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
                .body("[\"AB1\",\"AB2\"]")
                .post("/nomsNumbers")
                .then()
                .statusCode(200)
                .extract()
                .`as`(Array<OffenderDetail>::class.java)
        Assertions.assertThat(results).hasSize(0)
    }
}
