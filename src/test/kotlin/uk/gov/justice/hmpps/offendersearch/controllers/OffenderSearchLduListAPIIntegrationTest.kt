package uk.gov.justice.hmpps.offendersearch.controllers

import io.restassured.RestAssured
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.hmpps.offendersearch.dto.KeyValue
import uk.gov.justice.hmpps.offendersearch.dto.OffenderDetail

class OffenderSearchLduListAPIIntegrationTest : LocalstackIntegrationBase()  {

    @BeforeEach
    fun setUp() {
        loadOffenders(
                OffenderReplacement(
                        offenderManagers = listOf(
                                OffenderManagerReplacement(
                                        team = TeamReplacement(
                                                localDeliveryUnit =  KeyValue(code = "AAA123")
                                        )
                                )
                        )
                ))
//            ,
//                OffenderReplacement(
//                        offenderManagers = listOf(
//                                OffenderManagerReplacement(
//                                        team = TeamReplacement(
//                                                localDeliveryUnit = KeyValue(code = "BBB123")
//                                        )
//                                )
//                        )
//                ))
//                OffenderReplacement(
////                        deleted = true,
//                        offenderManagers = listOf(
//                                OffenderManagerReplacement(
//                                        team = TeamReplacement(
//                                                localDeliveryUnit = KeyValue(code = "CCC123")
//                                        )
//                                )
//                        )
//                )
//        )
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

//    @Test
//    fun lduCodeListSearch() {
//        val results = RestAssured.given()
//                .auth()
//                .oauth2(jwtAuthenticationHelper.createJwt("ROLE_COMMUNITY"))
//                .contentType(MediaType.APPLICATION_JSON_VALUE)
//                .body("""["AAA123","BBB123","CCC123", "DDD123"]""")
//                .post("/ldu-codes")
//                .then()
//                .statusCode(200)
//                .extract()
//                .body()
//                .`as`(Array<OffenderDetail>::class.java)
//
////        var res1 = results[0].offenderManagers?.get(0)?.team?.localDeliveryUnit?.code
////        var res2 = results[0].offenderManagers?.get(1)?.team?.localDeliveryUnit?.code
//////        var res3 = results[1].offenderManagers?.get(0)?.team?.localDeliveryUnit?.code
//////        var res4 = results[1].offenderManagers?.get(1)?.team?.localDeliveryUnit?.code
////
//////        Assertions.assertThat(res1).contains("DAA123")
//        Assertions.assertThat(results[0].offenderManagers?.get(0)?.team?.localDeliveryUnit?.code).contains("AAB123")
////        Assertions.assertThat(results[0].offenderManagers?.get(0)?.team?.localDeliveryUnit?.code == "BBB123")
//        Assertions.assertThat(results).hasSize(1)
//        Assertions.assertThat(results).extracting("offenderManagers.team.localDeliveryUnit.code").containsExactly("AAA123", "BBB123")
//    }
}