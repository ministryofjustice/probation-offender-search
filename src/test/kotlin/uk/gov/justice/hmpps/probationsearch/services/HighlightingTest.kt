package uk.gov.justice.hmpps.probationsearch.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.opensearch.common.text.Text
import org.opensearch.search.fetch.subphase.highlight.HighlightBuilder.Field
import org.opensearch.search.fetch.subphase.highlight.HighlightField
import uk.gov.justice.hmpps.probationsearch.dto.IDs
import uk.gov.justice.hmpps.probationsearch.dto.OffenderDetail
import java.time.LocalDate

internal class HighlightingTest {

  @Nested
  inner class BuildHighlightRequest {
    private val highlight = buildHighlightRequest()

    @Test
    internal fun `will not request any pre or post tags`() {
      assertThat(highlight.postTags()).isEqualTo(arrayOf(""))
      assertThat(highlight.preTags()).isEqualTo(arrayOf(""))
    }

    @Test
    internal fun `will highlight all fields where matched`() {
      assertThat(highlight.fields()).containsExactly(Field("*"))
    }
  }

  @Nested
  inner class TermHighlighterKtTest {
    @Test
    fun `will have no highlights if none found`() {
      val offenderDetail = OffenderDetail(offenderId = 99, otherIds = IDs("1234"))
      assertThat(offenderDetail.mergeHighlights(mapOf(), "smith").highlight).isEmpty()
    }

    @Test
    fun `will copy highlights for any fields found`() {
      val offenderDetail = OffenderDetail(offenderId = 99, otherIds = IDs("1234"))
      val highlights = mapOf(
        "surname" to HighlightField("surname", arrayOf(Text("Smith"))),
        "offenderAlias.surname" to HighlightField("surname", arrayOf(Text("smith"))),
      )
      assertThat(offenderDetail.mergeHighlights(highlightFields = highlights, phrase = "smith").highlight)
        .containsExactlyEntriesOf(
          mapOf(
            "surname" to listOf("Smith"),
            "offenderAlias.surname" to listOf("smith"),
          ),
        )
    }

    @Test
    fun `will highlight date of birth when format is same`() {
      val offenderDetail = OffenderDetail(otherIds = IDs("1234"), offenderId = 99, dateOfBirth = LocalDate.parse("1965-07-19"))
      val highlights = mapOf(
        "dateOfBirth" to HighlightField("dateOfBirth", arrayOf(Text("1965-07-19"))),
      )
      assertThat(offenderDetail.mergeHighlights(highlightFields = highlights, phrase = "1965-07-19").highlight)
        .containsExactlyEntriesOf(
          mapOf(
            "dateOfBirth" to listOf("1965-07-19"),
          ),
        )
    }

    @Test
    fun `will highlight date of birth even when format is different`() {
      val offenderDetail = OffenderDetail(offenderId = 99, otherIds = IDs("1234"), dateOfBirth = LocalDate.parse("1965-07-19"))
      assertThat(offenderDetail.mergeHighlights(mapOf(), "19/7/1965").highlight)
        .containsExactlyEntriesOf(
          mapOf(
            "dateOfBirth" to listOf("1965-07-19"),
          ),
        )
    }
  }
}
