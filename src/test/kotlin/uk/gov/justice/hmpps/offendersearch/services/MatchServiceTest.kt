package uk.gov.justice.hmpps.offendersearch.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.times
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
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.SearchHits
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import uk.gov.justice.hmpps.offendersearch.dto.MatchRequest
import uk.gov.justice.hmpps.offendersearch.dto.MatchedBy.ALL_SUPPLIED
import uk.gov.justice.hmpps.offendersearch.dto.MatchedBy.EXTERNAL_KEY
import uk.gov.justice.hmpps.offendersearch.dto.MatchedBy.HMPPS_KEY
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
  }

  @Nested
  inner class FullMatchAttempt {
    @BeforeEach
    fun setUp() {
      whenever(restHighLevelClient.search(any())).thenReturn(resultsOf(OffenderDetail(surname = "smith", offenderId = 99), OffenderDetail(surname = "smith", offenderId = 88)))
    }

    @Test
    fun `surname will be added to query when present`() {
      service.match(MatchRequest(surname = "smith"))

      verify(restHighLevelClient).search(check {
        assertThat(it.mustMultiMatchNames()).containsExactlyInAnyOrderEntriesOf(mapOf(
            "smith" to listOf("surname", "offenderAliases.surname").sorted()
        ))
        assertThat(it.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("softDeleted" to false))
      })
    }

    @Test
    fun `First name will be added to query when present`() {
      service.match(MatchRequest(surname = "smith", firstName = "John"))

      verify(restHighLevelClient).search(check {
        assertThat(it.mustMultiMatchNames()).containsExactlyInAnyOrderEntriesOf(mapOf(
            "smith" to listOf("surname", "offenderAliases.surname").sorted(),
            "John" to listOf("firstName", "offenderAliases.firstName").sorted()
        ))
        assertThat(it.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("softDeleted" to false))
      })
    }

    @Test
    fun `First name will be not be added to query when blank`() {
      service.match(MatchRequest(surname = "smith", firstName = ""))

      verify(restHighLevelClient).search(check {
        assertThat(it.mustMultiMatchNames()).containsExactlyInAnyOrderEntriesOf(mapOf(
            "smith" to listOf("surname", "offenderAliases.surname").sorted()
        ))
        assertThat(it.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("softDeleted" to false))
      })
    }

    @Test
    fun `Date of birth will be added to query when present`() {
      service.match(MatchRequest(surname = "smith", dateOfBirth = LocalDate.of(1965, 7, 19)))

      verify(restHighLevelClient).search(check {
        assertThat(it.mustMultiMatchNames()).containsExactlyInAnyOrderEntriesOf(mapOf(
            "smith" to listOf("surname", "offenderAliases.surname").sorted(),
            LocalDate.of(1965, 7, 19) to listOf("dateOfBirth", "offenderAliases.dateOfBirth").sorted()
        ))
        assertThat(it.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("softDeleted" to false))
      })
    }

    @Test
    fun `CRO number will be added as lowercase to query when present`() {
      service.match(MatchRequest(surname = "smith", croNumber = "SF80/655108T"))

      verify(restHighLevelClient).search(check {
        assertThat(it.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf(
            "otherIds.croNumberLowercase" to "sf80/655108t",
            "softDeleted" to false
        ))
      })
    }

    @Test
    fun `CRO number will be not be added to query when blank`() {
      service.match(MatchRequest(surname = "smith", croNumber = ""))

      verify(restHighLevelClient).search(check {
        assertThat(it.mustMultiMatchNames()).containsExactlyInAnyOrderEntriesOf(mapOf(
            "smith" to listOf("surname", "offenderAliases.surname").sorted()
        ))
        assertThat(it.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("softDeleted" to false))
      })
    }

    @Test
    fun `only PNC Number in canonical form will be added to query when present`() {
      service.match(MatchRequest(surname = "smith", pncNumber = "2018/0003456X"))

      verify(restHighLevelClient).search(check {
        assertThat(it.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("softDeleted" to false))
        assertThat(it.mustMultiMatchNames()).containsExactlyInAnyOrderEntriesOf(mapOf(
            "2018/3456x" to listOf("otherIds.pncNumberLongYear", "otherIds.pncNumberShortYear").sorted(),
            "smith" to listOf("surname", "offenderAliases.surname").sorted()
        ))
      })
    }

    @Test
    fun `PNC Number will be not be added to query when blank`() {
      service.match(MatchRequest(surname = "smith", pncNumber = ""))

      verify(restHighLevelClient).search(check {
        assertThat(it.mustMultiMatchNames()).containsExactlyInAnyOrderEntriesOf(mapOf(
            "smith" to listOf("surname", "offenderAliases.surname").sorted()
        ))
        assertThat(it.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("softDeleted" to false))
      })
    }

    @Test
    fun `NOMS Number will be added to query when present`() {
      service.match(MatchRequest(surname = "smith", nomsNumber = "G5555TT"))

      verify(restHighLevelClient).search(check {
        assertThat(it.mustMultiMatchNames()).containsExactlyInAnyOrderEntriesOf(mapOf(
            "smith" to listOf("surname", "offenderAliases.surname").sorted()
        ))
        assertThat(it.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf(
            "otherIds.nomsNumber" to "G5555TT",
            "softDeleted" to false
        ))
      })
    }

    @Test
    fun `NOMS Number will be not be added to query when blank`() {
      service.match(MatchRequest(surname = "smith", nomsNumber = ""))

      verify(restHighLevelClient).search(check {
        assertThat(it.mustMultiMatchNames()).containsExactlyInAnyOrderEntriesOf(mapOf(
            "smith" to listOf("surname", "offenderAliases.surname").sorted()
        ))
        assertThat(it.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("softDeleted" to false))
      })
    }

    @Test
    fun `disposal filter will be added to query when sentence filter present`() {
      service.match(MatchRequest(surname = "smith", activeSentence = true))

      verify(restHighLevelClient).search(check {
        assertThat(it.mustMultiMatchNames()).containsExactlyInAnyOrderEntriesOf(mapOf(
            "smith" to listOf("surname", "offenderAliases.surname").sorted()
        ))
        assertThat(it.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf(
            "currentDisposal" to "1",
            "softDeleted" to false
        ))
      })
    }

    @Test
    fun `disposal filter will be not be added to query when sentence filter not present`() {
      service.match(MatchRequest(surname = "smith", activeSentence = false))

      verify(restHighLevelClient).search(check {
        assertThat(it.mustMultiMatchNames()).containsExactlyInAnyOrderEntriesOf(mapOf(
            "smith" to listOf("surname", "offenderAliases.surname").sorted()
        ))
        assertThat(it.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("softDeleted" to false))
      })
    }
  }


  @Nested
  inner class CRONumberMatching {
    @BeforeEach
    fun setup() {
      whenever(restHighLevelClient.search(any()))
          .thenReturn(resultsOf()) // full match results
          .thenReturn(resultsOf(OffenderDetail(surname = "smith", offenderId = 99), OffenderDetail(surname = "smith", offenderId = 88)))
    }

    @Test
    fun `when CRO Number present only surname and date of birth will also be matched`() {
      service.match(MatchRequest(firstName = "john", surname = "smith", croNumber = "SF80/655108T", dateOfBirth = LocalDate.of(1995, 10, 4)))

      val searchRequestCaptor = argumentCaptor<SearchRequest>()

      verify(restHighLevelClient, times(2)).search(searchRequestCaptor.capture())

      assertThat(searchRequestCaptor.secondValue.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("otherIds.croNumberLowercase" to "sf80/655108t", "softDeleted" to false))
      assertThat(searchRequestCaptor.secondValue.nestedShouldMultiMatchNames()).containsExactlyInAnyOrderEntriesOf(
          mapOf(
              "smith" to listOf("surname", "offenderAliases.surname").sorted(),
              LocalDate.of(1995, 10, 4) to listOf("dateOfBirth", "offenderAliases.dateOfBirth").sorted()
          ))
    }
  }

  @Nested
  inner class PNCNumberMatch {
    @BeforeEach
    fun setup() {
      whenever(restHighLevelClient.search(any()))
          .thenReturn(resultsOf()) // full match results
          .thenReturn(resultsOf(OffenderDetail(surname = "smith", offenderId = 99), OffenderDetail(surname = "smith", offenderId = 88)))
    }

    @Test
    fun `when PNC Number present only surname and date of birth will also be matched`() {
      service.match(MatchRequest(firstName = "john", surname = "smith", pncNumber = "2018/0003456X", dateOfBirth = LocalDate.of(1995, 10, 4)))

      val searchRequestCaptor = argumentCaptor<SearchRequest>()

      verify(restHighLevelClient, times(2)).search(searchRequestCaptor.capture())

      assertThat(searchRequestCaptor.secondValue.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("softDeleted" to false))
      assertThat(searchRequestCaptor.secondValue.mustMultiMatchNames()).containsExactlyInAnyOrderEntriesOf(mapOf("2018/3456x" to listOf("otherIds.pncNumberLongYear", "otherIds.pncNumberShortYear")))
      assertThat(searchRequestCaptor.secondValue.nestedShouldMultiMatchNames()).containsExactlyInAnyOrderEntriesOf(
          mapOf(
              "smith" to listOf("surname", "offenderAliases.surname").sorted(),
              LocalDate.of(1995, 10, 4) to listOf("dateOfBirth", "offenderAliases.dateOfBirth").sorted()
          ))
    }
  }

  @Nested
  inner class NOMSNumberMatch {
    @BeforeEach
    fun setup() {
      whenever(restHighLevelClient.search(any()))
          .thenReturn(resultsOf()) // full match results
          .thenReturn(resultsOf(OffenderDetail(surname = "smith", offenderId = 99), OffenderDetail(surname = "smith", offenderId = 88)))
    }

    @Test
    fun `only NOMS Number will be added to query when present`() {
      service.match(MatchRequest(surname = "smith", nomsNumber = "G5555TT"))

      val searchRequestCaptor = argumentCaptor<SearchRequest>()

      verify(restHighLevelClient, times(2)).search(searchRequestCaptor.capture())

      assertThat(searchRequestCaptor.secondValue.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("otherIds.nomsNumber" to "G5555TT", "softDeleted" to false))
    }
  }

  @Nested
  inner class NameMatchAttempt {
    private val matchRequest = MatchRequest(
        firstName = "john",
        surname = "smith",
        dateOfBirth = LocalDate.of(1965, 7, 19),
        croNumber = "SF80/655108T",
        pncNumber = "2018/0003456X",
        nomsNumber = "G5555TT"
    )

    @BeforeEach
    fun setUp() {
      whenever(restHighLevelClient.search(any()))
          .thenReturn(resultsOf()) // full match results
          .thenReturn(resultsOf()) // NOMS Number results
          .thenReturn(resultsOf()) // CRO Number results
          .thenReturn(resultsOf()) // PNC Number results
          .thenReturn(resultsOf(OffenderDetail(surname = "smith", offenderId = 99), OffenderDetail(surname = "smith", offenderId = 88)))
    }

    @Test
    fun `names and date of birth will be added to query when present`() {
      service.match(matchRequest)

      val searchRequestCaptor = argumentCaptor<SearchRequest>()

      verify(restHighLevelClient, times(5)).search(searchRequestCaptor.capture())

      with(searchRequestCaptor.lastValue) {
        val query = source().query() as BoolQueryBuilder

        val (primaryNameQuery, aliasNameQuery) = query.must().filterIsInstance<BoolQueryBuilder>().first().should() as List<QueryBuilder>
        assertThat(primaryNameQuery.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf(
            "surname" to "smith",
            "firstName" to "john",
            "dateOfBirth" to LocalDate.of(1965, 7, 19)
        ))
        assertThat(aliasNameQuery.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf(
            "offenderAliases.surname" to "smith",
            "offenderAliases.firstName" to "john",
            "offenderAliases.dateOfBirth" to LocalDate.of(1965, 7, 19)
        ))
        assertThat(mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("softDeleted" to false))
      }
    }

    @Test
    fun `disposal filter will be added to query when sentence filter present`() {
      service.match(matchRequest.copy(activeSentence = true))

      val searchRequestCaptor = argumentCaptor<SearchRequest>()

      verify(restHighLevelClient, times(5)).search(searchRequestCaptor.capture())

      assertThat(searchRequestCaptor.lastValue.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf(
          "currentDisposal" to "1",
          "softDeleted" to false
      ))
    }

    @Test
    fun `disposal filter will be not be added to query when sentence filter not present`() {
      service.match(matchRequest.copy(activeSentence = false))

      val searchRequestCaptor = argumentCaptor<SearchRequest>()

      verify(restHighLevelClient, times(5)).search(searchRequestCaptor.capture())

      assertThat(searchRequestCaptor.lastValue.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("softDeleted" to false))
    }
  }

  @Nested
  inner class PartialNameMatchAttempt {
    private val matchRequest = MatchRequest(
        firstName = "john",
        surname = "smith",
        dateOfBirth = LocalDate.of(1965, 7, 19),
        croNumber = "SF80/655108T",
        pncNumber = "2018/0003456X",
        nomsNumber = "G5555TT"
    )

    @BeforeEach
    fun setUp() {
      whenever(restHighLevelClient.search(any()))
          .thenReturn(resultsOf()) // full match results
          .thenReturn(resultsOf()) // NOMS Number results
          .thenReturn(resultsOf()) // CRO Number results
          .thenReturn(resultsOf()) // PNC Number results
          .thenReturn(resultsOf()) // name match results
          .thenReturn(resultsOf(OffenderDetail(surname = "smith", offenderId = 99), OffenderDetail(surname = "smith", offenderId = 88)))
    }

    @Test
    fun `names and date of birth will be added to query when present`() {
      service.match(matchRequest)

      val searchRequestCaptor = argumentCaptor<SearchRequest>()

      verify(restHighLevelClient, times(6)).search(searchRequestCaptor.capture())

      assertThat(searchRequestCaptor.lastValue.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf(
          "surname" to "smith",
          "dateOfBirth" to LocalDate.of(1965, 7, 19),
          "softDeleted" to false
      ))
    }
  }

  @Nested
  inner class PartialNameDateOfBirthLenientMatchAttempt {

    @BeforeEach
    fun setUp() {
      whenever(restHighLevelClient.search(any()))
          .thenReturn(resultsOf()) // full match results
          .thenReturn(resultsOf()) // NOMS Number results
          .thenReturn(resultsOf()) // CRO Number results
          .thenReturn(resultsOf()) // PNC Number results
          .thenReturn(resultsOf()) // name match results
          .thenReturn(resultsOf()) // partial name match results
          .thenReturn(resultsOf(OffenderDetail(surname = "smith", offenderId = 99), OffenderDetail(surname = "smith", offenderId = 88)))
    }

    @Test
    fun `names and date of birth variations will be added to query when present`() {
      service.match(MatchRequest(
          firstName = "john",
          surname = "smith",
          dateOfBirth = LocalDate.of(1965, 7, 19),
          croNumber = "SF80/655108T",
          pncNumber = "2018/0003456X",
          nomsNumber = "G5555TT"
      ))

      val searchRequestCaptor = argumentCaptor<SearchRequest>()

      verify(restHighLevelClient, times(7)).search(searchRequestCaptor.capture())

      with(searchRequestCaptor.lastValue) {
        assertThat(mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf(
            "surname" to "smith",
            "softDeleted" to false
        ))
        val query = source().query() as BoolQueryBuilder
        val dateMatches = query.must().filterIsInstance<BoolQueryBuilder>().first().should()
        val matchingDates = dateMatches.map { it.mustNames() }.flatMap { it.values }
        assertThat(matchingDates).containsExactlyInAnyOrder(
            "1965-01-19".toLocalDate(),
            "1965-02-19".toLocalDate(),
            "1965-03-19".toLocalDate(),
            "1965-04-19".toLocalDate(),
            "1965-05-19".toLocalDate(),
            "1965-06-19".toLocalDate(),
            "1965-07-18".toLocalDate(),
            "1965-07-19".toLocalDate(),
            "1965-07-20".toLocalDate(),
            "1965-08-19".toLocalDate(),
            "1965-09-19".toLocalDate(),
            "1965-10-19".toLocalDate(),
            "1965-11-19".toLocalDate(),
            "1965-12-19".toLocalDate()
        )

        assertThat(mustMultiMatchNames()).containsExactlyInAnyOrderEntriesOf(mapOf(
            "john" to listOf("firstName", "offenderAliases.firstName").sorted()
        ))
      }
    }

    @Test
    fun `invalid date variations will not be tested`() {
      service.match(MatchRequest(
          firstName = "john",
          surname = "smith",
          dateOfBirth = LocalDate.of(1965, 2, 28),
          croNumber = "SF80/655108T",
          pncNumber = "2018/0003456X",
          nomsNumber = "G5555TT"
      ))

      val searchRequestCaptor = argumentCaptor<SearchRequest>()
      verify(restHighLevelClient, times(7)).search(searchRequestCaptor.capture())

      val query = searchRequestCaptor.lastValue.source().query() as BoolQueryBuilder
      val dateMatches = query.must().filterIsInstance<BoolQueryBuilder>().first().should()
      val matchingDates = dateMatches.map { it.mustNames() }.flatMap { it.values }
      assertThat(matchingDates).containsExactlyInAnyOrder(
          "1965-01-28".toLocalDate(),
          "1965-02-27".toLocalDate(),
          "1965-02-28".toLocalDate(),
          "1965-03-28".toLocalDate(),
          "1965-04-28".toLocalDate(),
          "1965-05-28".toLocalDate(),
          "1965-06-28".toLocalDate(),
          "1965-07-28".toLocalDate(),
          "1965-08-28".toLocalDate(),
          "1965-09-28".toLocalDate(),
          "1965-10-28".toLocalDate(),
          "1965-11-28".toLocalDate(),
          "1965-12-28".toLocalDate()
      )
    }
  }

  @Nested
  inner class Results {
    val matchRequest = MatchRequest(
        firstName = "john",
        surname = "smith",
        dateOfBirth = LocalDate.of(1965, 7, 19),
        croNumber = "SF80/655108T",
        pncNumber = "2018/0003456X",
        nomsNumber = "G5555TT"
    )

    @Test
    fun `will return matches`() {
      whenever(restHighLevelClient.search(any())).thenReturn(resultsOf(
          OffenderDetail(surname = "smith", offenderId = 99),
          OffenderDetail(surname = "smith", offenderId = 88)
      ))

      val results = service.match(matchRequest)
      assertThat(results.matches).hasSize(2)
    }

    @Test
    fun `will return matched by ALL_MATCHED when matching all parameters`() {
      whenever(restHighLevelClient.search(any())).thenReturn(resultsOf(
          OffenderDetail(surname = "smith", offenderId = 99)
      ))

      val results = service.match(matchRequest)
      assertThat(results.matchedBy).isEqualTo(ALL_SUPPLIED)
    }

    @Test
    fun `will return matched by HMPPS_KEY when matching on NOMS number`() {
      whenever(restHighLevelClient.search(any()))
          .thenReturn(resultsOf()) // full match
          .thenReturn(resultsOf(OffenderDetail(surname = "smith", offenderId = 99)
          ))

      val results = service.match(matchRequest)
      assertThat(results.matchedBy).isEqualTo(HMPPS_KEY)
    }

    @Test
    fun `will return matched by EXTERNAL_KEY when matching on CRO number`() {
      whenever(restHighLevelClient.search(any()))
          .thenReturn(resultsOf()) // full match
          .thenReturn(resultsOf()) // NOMS number match
          .thenReturn(resultsOf(OffenderDetail(surname = "smith", offenderId = 99)
          ))

      val results = service.match(matchRequest)
      assertThat(results.matchedBy).isEqualTo(EXTERNAL_KEY)
    }

    @Test
    fun `will return matched by EXTERNAL_KEY when matching on PNC number`() {
      whenever(restHighLevelClient.search(any()))
          .thenReturn(resultsOf()) // full match
          .thenReturn(resultsOf()) // NOMS number match
          .thenReturn(resultsOf()) // CRO number match
          .thenReturn(resultsOf(OffenderDetail(surname = "smith", offenderId = 99)
          ))

      val results = service.match(matchRequest)
      assertThat(results.matchedBy).isEqualTo(EXTERNAL_KEY)
    }
  }

  private fun resultsOf(vararg offenders: OffenderDetail): SearchResponse {
    val searchHits = offenders.map { SearchHit(it.offenderId.toInt()).apply { sourceRef(BytesArray(objectMapper.writeValueAsBytes(it))) } }
    val hits = SearchHits(searchHits.toTypedArray(), offenders.size.toLong(), 10f)
    val searchResponseSections = SearchResponseSections(hits, null, null, false, null, null, 5)
    return SearchResponse(searchResponseSections, null, 8, 8, 0, 8, arrayOf())
  }

}

private fun String.toLocalDate(): LocalDate = LocalDate.parse(this)

fun SearchRequest.mustNames(): Map<String, Any> {
  val query = source().query() as BoolQueryBuilder
  return query.must().filterIsInstance<MatchQueryBuilder>().map { it.fieldName() to it.value() }.toMap()
}

fun SearchRequest.nestedShouldMultiMatchNames(): Map<Any, List<String>> {
  val query = source().query() as BoolQueryBuilder
  return query.must().filterIsInstance<BoolQueryBuilder>().flatMap { it.should().filterIsInstance<MultiMatchQueryBuilder>() }.map { it.value() to it.fields().keys.toList().sorted() }.toMap()
}

fun SearchRequest.mustMultiMatchNames(): Map<Any, List<String>> {
  val query = source().query() as BoolQueryBuilder
  return query.must().filterIsInstance<MultiMatchQueryBuilder>().map { it.value() to it.fields().keys.toList().sorted() }.toMap()
}

fun QueryBuilder.mustNames(): Map<String, Any> {
  return when(this) {
      is BoolQueryBuilder -> this.must().filterIsInstance<MatchQueryBuilder>().map { it.fieldName() to it.value() }.toMap()
    else -> mapOf()
  }
}