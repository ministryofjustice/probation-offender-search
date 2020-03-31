package uk.gov.justice.hmpps.offendersearch.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.search.SearchResponseSections
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.common.bytes.BytesArray
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.MatchQueryBuilder
import org.elasticsearch.index.query.MultiMatchQueryBuilder
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.SearchHits
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import uk.gov.justice.hmpps.offendersearch.dto.MatchRequest
import uk.gov.justice.hmpps.offendersearch.dto.OffenderDetail
import java.time.LocalDate


@ExtendWith(MockitoExtension::class)
internal class MatchServiceTest {

  private lateinit var service: MatchService

  private val objectMapper: ObjectMapper = jacksonObjectMapper()


  @Mock
  lateinit var restHighLevelClient: RestHighLevelClient

  @BeforeEach
  fun setUp() {
    service = MatchService(restHighLevelClient, objectMapper)
    whenever(restHighLevelClient.search(any())).thenReturn(resultsOf(OffenderDetail(surname = "smith", offenderId = 99), OffenderDetail(surname = "smith", offenderId = 88)))
  }

  @Test
  fun `surname will be added to query when present`() {
    service.match(MatchRequest(surname = "smith"))

    verify(restHighLevelClient).search(check {
      assertThat(it.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("surname" to "smith", "softDeleted" to false))
    })
  }

  @Test
  fun `First name will be added to query when present`() {
    service.match(MatchRequest(surname = "smith", firstName = "John"))

    verify(restHighLevelClient).search(check {
      assertThat(it.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("surname" to "smith", "firstName" to "John", "softDeleted" to false))
    })
  }

  @Test
  fun `First name will be not be added to query when blank`() {
    service.match(MatchRequest(surname = "smith", firstName = ""))

    verify(restHighLevelClient).search(check {
      assertThat(it.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("surname" to "smith", "softDeleted" to false))
    })
  }

  @Test
  fun `Date of birth will be added to query when present`() {
    service.match(MatchRequest(surname = "smith", dateOfBirth = LocalDate.of(1965, 7, 19)))

    verify(restHighLevelClient).search(check {
      assertThat(it.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("surname" to "smith", "dateOfBirth" to LocalDate.of(1965, 7, 19), "softDeleted" to false))
    })
  }

  @Test
  fun `CRO will be added as lowercase to query when present`() {
    service.match(MatchRequest(surname = "smith", croNumber = "SF80/655108T"))

    verify(restHighLevelClient).search(check {
      assertThat(it.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("otherIds.croNumberLowercase" to "sf80/655108t", "softDeleted" to false))
      assertThat(it.nestedShouldMultiMatchNames()).containsExactlyInAnyOrderEntriesOf(mapOf("smith" to listOf("surname", "offenderAliases.surname").sorted()))
    })
  }

  @Test
  fun `when CRO present only surname and date of birth will also be matched`() {
    service.match(MatchRequest(firstName = "john",  surname = "smith", croNumber = "SF80/655108T", dateOfBirth = LocalDate.of(1995, 10, 4)))

    verify(restHighLevelClient).search(check {
      assertThat(it.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("otherIds.croNumberLowercase" to "sf80/655108t", "softDeleted" to false))
      assertThat(it.nestedShouldMultiMatchNames()).containsExactlyInAnyOrderEntriesOf(
          mapOf(
              "smith" to listOf("surname", "offenderAliases.surname").sorted(),
              LocalDate.of(1995, 10, 4) to listOf("dateOfBirth", "offenderAliases.dateOfBirth").sorted()
          ))
    })
  }

  @Test
  fun `CRO will be not be added to query when blank`() {
    service.match(MatchRequest(surname = "smith", croNumber = ""))

    verify(restHighLevelClient).search(check {
      assertThat(it.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("surname" to "smith", "softDeleted" to false))
    })
  }

  @Test
  fun `only PNC Number in canonical form will be added to query when present`() {
    service.match(MatchRequest(surname = "smith", pncNumber = "2018/0003456X"))

    verify(restHighLevelClient).search(check {
      assertThat(it.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("softDeleted" to false))
      assertThat(it.mustMultiMatchNames()).containsExactlyInAnyOrderEntriesOf(mapOf("2018/3456x" to listOf("otherIds.pncNumberLongYear", "otherIds.pncNumberShortYear")))
      assertThat(it.nestedShouldMultiMatchNames()).containsExactlyInAnyOrderEntriesOf(mapOf("smith" to listOf("surname", "offenderAliases.surname").sorted()))
    })
  }

  @Test
  fun `when PNC present only surname and date of birth will also be matched`() {
    service.match(MatchRequest(firstName = "john",  surname = "smith", pncNumber = "2018/0003456X", dateOfBirth = LocalDate.of(1995, 10, 4)))

    verify(restHighLevelClient).search(check {
      assertThat(it.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("softDeleted" to false))
      assertThat(it.mustMultiMatchNames()).containsExactlyInAnyOrderEntriesOf(mapOf("2018/3456x" to listOf("otherIds.pncNumberLongYear", "otherIds.pncNumberShortYear")))
      assertThat(it.nestedShouldMultiMatchNames()).containsExactlyInAnyOrderEntriesOf(
          mapOf(
              "smith" to listOf("surname", "offenderAliases.surname").sorted(),
              LocalDate.of(1995, 10, 4) to listOf("dateOfBirth", "offenderAliases.dateOfBirth").sorted()
          ))

    })
  }

  @Test
  fun `PNC will be not be added to query when blank`() {
    service.match(MatchRequest(surname = "smith", pncNumber = ""))

    verify(restHighLevelClient).search(check {
      assertThat(it.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("surname" to "smith", "softDeleted" to false))
    })
  }

  @Test
  fun `only NOMS Number will be added to query when present`() {
    service.match(MatchRequest(surname = "smith", nomsNumber = "G5555TT"))

    verify(restHighLevelClient).search(check {
      assertThat(it.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("otherIds.nomsNumber" to "G5555TT", "softDeleted" to false))
    })
  }

  @Test
  fun `NOMS Number will be not be added to query when blank`() {
    service.match(MatchRequest(surname = "smith", nomsNumber = ""))

    verify(restHighLevelClient).search(check {
      assertThat(it.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("surname" to "smith", "softDeleted" to false))
    })
  }

  @Test
  fun `disposal filter will be added to query when sentence filter present`() {
    service.match(MatchRequest(surname = "smith", activeSentence = true))

    verify(restHighLevelClient).search(check {
      assertThat(it.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("surname" to "smith", "currentDisposal" to "1", "softDeleted" to false))
    })
  }

  @Test
  fun `disposal filter will be not be added to query when sentence filter not present`() {
    service.match(MatchRequest(surname = "smith", activeSentence = false))

    verify(restHighLevelClient).search(check {
      assertThat(it.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("surname" to "smith", "softDeleted" to false))
    })
  }

  @Test
  @MockitoSettings(strictness = Strictness.LENIENT)
  fun `will return matches`() {
    whenever(restHighLevelClient.search(any())).thenReturn(resultsOf(
        OffenderDetail(surname = "smith", offenderId = 99),
        OffenderDetail(surname = "smith", offenderId = 88)
    ))

    val results = service.match(MatchRequest(surname = "smith"))
    assertThat(results.matches).hasSize(2)
  }

  private fun resultsOf(vararg offenders: OffenderDetail): SearchResponse {
    val searchHits = offenders.map { SearchHit(it.offenderId.toInt()).apply { sourceRef(BytesArray(objectMapper.writeValueAsBytes(it))) } }
    val hits = SearchHits(searchHits.toTypedArray(), offenders.size.toLong(), 10f)
    val searchResponseSections = SearchResponseSections(hits, null, null, false, null, null, 5)
    return SearchResponse(searchResponseSections, null, 8, 8, 0, 8, arrayOf())
  }

}

fun SearchRequest.mustNames(): Map<String, Any> {
  val query = source().query() as BoolQueryBuilder
  return query.must().filterIsInstance<MatchQueryBuilder>().map { it.fieldName() to it.value() }.toMap()
}

fun SearchRequest.nestedShouldMultiMatchNames(): Map<Any, List<String>> {
  val query = source().query() as BoolQueryBuilder
  return query.must().filterIsInstance<BoolQueryBuilder>().flatMap { it.should().filterIsInstance<MultiMatchQueryBuilder>() }.map { it.value() to it.fields().keys.toList().sorted()  }.toMap()
}

fun SearchRequest.mustMultiMatchNames(): Map<String, Any> {
  val query = source().query() as BoolQueryBuilder
  return query.must().filterIsInstance<MultiMatchQueryBuilder>().map { it.value() as String to it.fields().keys.toList().sorted() }.toMap()
}
