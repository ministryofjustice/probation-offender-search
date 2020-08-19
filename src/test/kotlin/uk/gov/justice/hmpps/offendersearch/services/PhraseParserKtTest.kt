package uk.gov.justice.hmpps.offendersearch.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class PhraseParserKtTest {

  @Nested
  inner class ExtractDateLikeTerms {
    @Test
    internal fun `will extract a date in canonical format`() {
      val dates = extractDateLikeTerms("19/07/1965")
      assertThat(dates).containsExactly("1965-07-19")
    }

    @Test
    internal fun `will extract a dates ignoring other terms`() {
      val dates = extractDateLikeTerms("john smith 19/07/1965")
      assertThat(dates).containsExactly("1965-07-19")
    }

    @Test
    internal fun `will only extract dates`() {
      val dates = extractDateLikeTerms("john smith")
      assertThat(dates).isEmpty()
    }

    @Test
    internal fun `will only extract dates that are valid`() {
      val dates = extractDateLikeTerms("19th-Augist-1987")
      assertThat(dates).isEmpty()
    }

    @Test
    internal fun `will not extract dates when phrase is empty`() {
      val dates = extractDateLikeTerms("")
      assertThat(dates).isEmpty()
    }

    @Test
    internal fun `will extract all dates`() {
      val dates = extractDateLikeTerms("19/07/1965 18/08/1965")
      assertThat(dates).containsExactly("1965-07-19", "1965-08-18")
    }

    @Test
    internal fun `will extract all dates separated by loads of spaces`() {
      val dates = extractDateLikeTerms("19/07/1965            18/08/1965")
      assertThat(dates).containsExactly("1965-07-19", "1965-08-18")
    }

    @Test
    internal fun `will extract dates in most formats`() {
      val dates = extractDateLikeTerms("1965/07/19 1-Aug-1965")
      assertThat(dates).containsExactly("1965-07-19", "1965-08-01")
    }
  }

  @Nested
  inner class ExtractPNCNumberLikeTerms {
    @Test
    internal fun `will extract a pnc number in canonical format`() {
      val pncNumbers = extractPNCNumberLikeTerms("2003/0034567A")
      assertThat(pncNumbers).containsExactly("2003/34567a")
    }

    @Test
    internal fun `will extract a pnc number ignoring other terms`() {
      val pncNumbers = extractPNCNumberLikeTerms("john smith 2003/0034567A")
      assertThat(pncNumbers).containsExactly("2003/34567a")
    }

    @Test
    internal fun `will only extract pnc numbers`() {
      val pncNumbers = extractPNCNumberLikeTerms("john smith")
      assertThat(pncNumbers).isEmpty()
    }

    @Test
    internal fun `will only extract pnc numbers that are valid`() {
      val pncNumbers = extractPNCNumberLikeTerms("2003/003456788")
      assertThat(pncNumbers).isEmpty()
    }

    @Test
    internal fun `will not extract pnc numbers when phrase is empty`() {
      val pncNumbers = extractPNCNumberLikeTerms("")
      assertThat(pncNumbers).isEmpty()
    }

    @Test
    internal fun `will extract all pnc numbers`() {
      val pncNumbers = extractPNCNumberLikeTerms("2003/1234567A 1999/1234567A")
      assertThat(pncNumbers).containsExactly("2003/1234567a", "1999/1234567a")
    }

    @Test
    internal fun `will extract all pnc numbers separated by loads of spaces`() {
      val pncNumbers = extractPNCNumberLikeTerms("2003/1234567A            03/00001A")
      assertThat(pncNumbers).containsExactly("2003/1234567a", "03/1a")
    }

    @Test
    internal fun `will extract pnc numbers in most formats`() {
      val pncNumbers = extractPNCNumberLikeTerms("2003/1234567A 2003/0000001A 03/1234567A 03/00001A")
      assertThat(pncNumbers).containsExactly("2003/1234567a", "2003/1a", "03/1234567a", "03/1a")
    }

  }

  @Nested
  inner class ExtractCRONumberLikeTerms {
    @Test
    internal fun `will extract a cro number in canonical format`() {
      val croNumbers = extractCRONumberLikeTerms("123456/99a")
      assertThat(croNumbers).containsExactly("123456/99a")
    }

    @Test
    internal fun `will extract a cro number ignoring other terms`() {
      val croNumbers = extractCRONumberLikeTerms("john smith 123456/99a")
      assertThat(croNumbers).containsExactly("123456/99a")
    }

    @Test
    internal fun `will only extract cro numbers`() {
      val croNumbers = extractCRONumberLikeTerms("john smith")
      assertThat(croNumbers).isEmpty()
    }

    @Test
    internal fun `will only extract cro numbers that are valid`() {
      val croNumbers = extractCRONumberLikeTerms("1234567/98A")
      assertThat(croNumbers).isEmpty()
    }

    @Test
    internal fun `will not extract cro numbers when phrase is empty`() {
      val croNumbers = extractCRONumberLikeTerms("")
      assertThat(croNumbers).isEmpty()
    }

    @Test
    internal fun `will extract all cro numbers`() {
      val croNumbers = extractCRONumberLikeTerms("123456/99A 123456/08a")
      assertThat(croNumbers).containsExactly("123456/99a", "123456/08a")
    }

    @Test
    internal fun `will extract all cro numbers separated by loads of spaces`() {
      val croNumbers = extractCRONumberLikeTerms("123456/99A            123456/08a")
      assertThat(croNumbers).containsExactly("123456/99a", "123456/08a")
    }

    @Test
    internal fun `will extract cro numbers in most formats`() {
      val croNumbers = extractCRONumberLikeTerms("123456/99Z 1/99Z SF94/123456A SF94/1A")
      assertThat(croNumbers).containsExactly("123456/99z", "1/99z", "sf94/123456a", "sf94/1a")
    }


  }

  @Nested
  inner class ExtractSearchableSimpleTerms {
    @Test
    internal fun `will extract terms that are not dates`() {
      val terms = extractSearchableSimpleTerms("19/07/1965 1-Aug-1965 john smith")
      assertThat(terms).isEqualTo("john smith")
    }

    @Test
    internal fun `will convert terms to lowercase`() {
      val terms = extractSearchableSimpleTerms("John SMITH")
      assertThat(terms).isEqualTo("john smith")
    }

    @Test
    internal fun `will ignore terms that are just single letters`() {
      val terms = extractSearchableSimpleTerms("j b SMITH")
      assertThat(terms).isEqualTo("smith")
    }

    @Test
    internal fun `will ignore terms that look like references with slashes in`() {
      val terms = extractSearchableSimpleTerms("12345/99Z 2003/0004567A 20/20 smith")
      assertThat(terms).isEqualTo("smith")
    }

    @Test
    internal fun `will extract even with all combinations`() {
      val terms = extractSearchableSimpleTerms("12345/99Z 2003/0004567A 20/20 SmItH a 19/5/1987 2001-01-12")
      assertThat(terms).isEqualTo("smith")
    }
  }

  @Nested
  inner class ExtractSearchableSimpleTermsWithSingleLetters {
    @Test
    internal fun `will extract terms that are not dates`() {
      val terms = extractSearchableSimpleTermsWithSingleLetters("19/07/1965 1-Aug-1965 j smith")
      assertThat(terms).containsExactly("j", "smith")
    }

    @Test
    internal fun `will convert terms to lowercase`() {
      val terms = extractSearchableSimpleTermsWithSingleLetters("J SMITH")
      assertThat(terms).containsExactly("j", "smith")
    }

    @Test
    internal fun `will not ignore terms that are just single letters`() {
      val terms = extractSearchableSimpleTermsWithSingleLetters("j b SMITH")
      assertThat(terms).containsExactly("j", "b", "smith")
    }

    @Test
    internal fun `will ignore terms that look like references with slashes in`() {
      val terms = extractSearchableSimpleTermsWithSingleLetters("12345/99Z 2003/0004567A 20/20 j smith")
      assertThat(terms).containsExactly("j", "smith")
    }

    @Test
    internal fun `will extract even with all combinations`() {
      val terms = extractSearchableSimpleTermsWithSingleLetters("12345/99Z 2003/0004567A 20/20 SmItH a 19/5/1987 2001-01-12")
      assertThat(terms).containsExactly("smith", "a")
    }
  }
}