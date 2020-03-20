package uk.gov.justice.hmpps.offendersearch.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.check
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.search.SearchResponseSections
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.common.bytes.BytesArray
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.MatchQueryBuilder
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.SearchHits
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.hmpps.offendersearch.dto.MatchRequest
import uk.gov.justice.hmpps.offendersearch.dto.OffenderDetail
import java.time.LocalDate


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@ExtendWith(SpringExtension::class)
internal class MatchServiceTest {

  lateinit var service: MatchService

  @Autowired
  private lateinit var objectMapper: ObjectMapper


  @Mock
  lateinit var restHighLevelClient: RestHighLevelClient

  @BeforeEach
  fun setUp() {
    service = MatchService(restHighLevelClient, objectMapper)
    whenever(restHighLevelClient.search(any())).thenReturn(resultsOf())
  }

  @Test
  fun `surname will be added to query when present`() {
    service.match(MatchRequest(surname = "smith"))

    verify(restHighLevelClient).search(check {
      assertThat(it.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("surname" to "smith"))
    })
  }

  @Test
  fun `First name will be added to query when present`() {
    service.match(MatchRequest(surname = "smith", firstName = "John"))

    verify(restHighLevelClient).search(check {
      assertThat(it.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("surname" to "smith", "firstName" to "John"))
    })
  }

  @Test
  fun `First name will be not be added to query when blank`() {
    service.match(MatchRequest(surname = "smith", firstName = ""))

    verify(restHighLevelClient).search(check {
      assertThat(it.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("surname" to "smith"))
    })
  }

  @Test
  fun `Date of birth will be added to query when present`() {
    service.match(MatchRequest(surname = "smith", dateOfBirth = LocalDate.of(1965, 7, 19)))

    verify(restHighLevelClient).search(check {
      assertThat(it.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("surname" to "smith", "dateOfBirth" to LocalDate.of(1965, 7, 19)))
    })
  }

  @Test
  fun `CRO will be added to query when present`() {
    service.match(MatchRequest(surname = "smith", croNumber = "SF80/655108T"))

    verify(restHighLevelClient).search(check {
      assertThat(it.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("surname" to "smith", "otherIds.croNumber" to "SF80/655108T"))
    })
  }

  @Test
  fun `CRO will be not be added to query when blank`() {
    service.match(MatchRequest(surname = "smith", croNumber = ""))

    verify(restHighLevelClient).search(check {
      assertThat(it.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("surname" to "smith"))
    })
  }

  @Test
  fun `PNC will be added to query when present`() {
    service.match(MatchRequest(surname = "smith", pncNumber = "2018/0123456X"))

    verify(restHighLevelClient).search(check {
      assertThat(it.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("surname" to "smith", "otherIds.pncNumber" to "2018/0123456X"))
    })
  }

  @Test
  fun `PNC will be not be added to query when blank`() {
    service.match(MatchRequest(surname = "smith", pncNumber = ""))

    verify(restHighLevelClient).search(check {
      assertThat(it.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("surname" to "smith"))
    })
  }

  @Test
  fun `NOMS Number will be added to query when present`() {
    service.match(MatchRequest(surname = "smith", nomsNumber = "G5555TT"))

    verify(restHighLevelClient).search(check {
      assertThat(it.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("surname" to "smith", "otherIds.nomsNumber" to "G5555TT"))
    })
  }

  @Test
  fun `NOMS Number will be not be added to query when blank`() {
    service.match(MatchRequest(surname = "smith", nomsNumber = ""))

    verify(restHighLevelClient).search(check {
      assertThat(it.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("surname" to "smith"))
    })
  }

  @Test
  fun `disposal filter will be added to query when sentence filter present`() {
    service.match(MatchRequest(surname = "smith", activeSentence = true))

    verify(restHighLevelClient).search(check {
      assertThat(it.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("surname" to "smith", "currentDisposal" to "1"))
    })
  }

  @Test
  fun `disposal filter will be not be added to query when sentence filter not present`() {
    service.match(MatchRequest(surname = "smith", activeSentence = false))

    verify(restHighLevelClient).search(check {
      assertThat(it.mustNames()).containsExactlyInAnyOrderEntriesOf(mapOf("surname" to "smith"))
    })
  }

  @Test
  fun `will return matches`() {
    whenever(restHighLevelClient.search(any())).thenReturn(resultsOf(OffenderDetail(surname = "smith", offenderId = 99), OffenderDetail(surname = "smith", offenderId = 88)))

    val results = service.match(MatchRequest(surname = "smith"))
    assertThat(results.matches).hasSize(2)
  }

  private fun resultsOf(vararg offenders: OffenderDetail): SearchResponse {
    val searchHits = offenders.map { SearchHit(it.offenderId?.toInt() ?: 99).apply { sourceRef(BytesArray(objectMapper.writeValueAsBytes(it))) } }
    val hits = SearchHits(searchHits.toTypedArray(), offenders.size.toLong(), 10f)
    val searchResponseSections = SearchResponseSections(hits, null, null, false, null, null, 5)
    return SearchResponse(searchResponseSections, null, 8, 8, 0, 8, arrayOf())
  }

}

fun SearchRequest.mustNames(): Map<String, Any> {
  val query = source().query() as BoolQueryBuilder
  return query.must().map { (it as MatchQueryBuilder).fieldName() to it.value() }.toMap()
}