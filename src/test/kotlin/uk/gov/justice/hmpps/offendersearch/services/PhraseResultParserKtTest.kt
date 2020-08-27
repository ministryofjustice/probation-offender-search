package uk.gov.justice.hmpps.offendersearch.services

import com.google.gson.Gson
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.elasticsearch.search.SearchHit
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.hmpps.offendersearch.dto.OffenderDetail

internal class PhraseResultParserKtTest {
  private val searchHit: SearchHit = mock()

  @BeforeEach
  internal fun setUp() {
    whenever(searchHit.sourceAsString).thenReturn("""
      {
        "offenderId": 99,
        "firstName": "John",
        "surname": "Smith",
        "otherIds": {
          "crn": "X00001"
         },
         "offenderManagers": [
           {
             "allocationReason": {
               "code": "IA",
               "description": "Inactive Offender"
             },
             "partitionArea": "National Data",
             "trustOfficer": {
               "surname": "Staff",
               "forenames": "Inactive Staff(N02)"
             },
             "active": true,
             "staff": {
               "surname": "Staff",
               "forenames": "Inactive Staff(N02)"
             },
             "team": {
               "district": {
                 "code": "N02IAV",
                 "description": "Inactive Level 3(N02)"
               },
               "description": "Inactive Team(N02)",
               "borough": {
                 "code": "N02IAV",
                 "description": "Inactive Level 2(N02)"
               }
             },
             "softDeleted": false,
             "probationArea": {
               "code": "N02",
               "description": "NPS North East"
             }
           }
         ]
      }
    """.trimIndent())
    whenever(searchHit.highlightFields).thenReturn(mapOf())
  }

  @Test
  internal fun `will transform to list of offenders`() {
    val offenders = extractOffenderDetailList(
        hits = arrayOf(searchHit),
        phrase = "john smith",
        offenderParser = ::offenderParser,
        accessChecker = { _ -> true }
    )

    assertThat(offenders).hasSize(1)
    assertThat(offenders[0].firstName).isEqualTo("John")
    assertThat(offenders[0].surname).isEqualTo("Smith")
    assertThat(offenders[0].otherIds?.crn).isEqualTo("X00001")
    assertThat(offenders[0].offenderManagers).hasSize(1)
  }

  @Test
  internal fun `will redact details when offender not accessible`() {
    val offenders = extractOffenderDetailList(
        hits = arrayOf(searchHit),
        phrase = "john smith",
        offenderParser = ::offenderParser,
        accessChecker = { false }
    )

    assertThat(offenders).hasSize(1)
    assertThat(offenders[0].firstName).isNull()
    assertThat(offenders[0].surname).isNull()
    assertThat(offenders[0].accessDenied).isTrue
  }
  @Test
  internal fun `redacted offenders will have crn, offenderId and offender managers`() {
    val offenders = extractOffenderDetailList(
        hits = arrayOf(searchHit),
        phrase = "john smith",
        offenderParser = ::offenderParser,
        accessChecker = { false }
    )

    assertThat(offenders[0].offenderId).isEqualTo(99)
    assertThat(offenders[0].otherIds?.crn).isEqualTo("X00001")
    assertThat(offenders[0].offenderManagers).hasSize(1)
  }
}

fun offenderParser(json: String): OffenderDetail = Gson().fromJson(json, OffenderDetail::class.java)