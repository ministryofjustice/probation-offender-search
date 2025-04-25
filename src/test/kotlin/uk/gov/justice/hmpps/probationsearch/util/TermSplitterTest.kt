package uk.gov.justice.hmpps.probationsearch.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.hmpps.probationsearch.utils.TermSplitter

internal class TermSplitterTest {
  companion object {
    @JvmStatic
    fun terms() = listOf(
      arguments("Two words", 2),
      arguments("", 0),
      arguments(
        """
        word
        
        next
        
        another
        
        Spec!al Ch.&r 
      """.trimIndent(),
        5,
      ),
    )
  }

  @ParameterizedTest
  @MethodSource("terms")
  internal fun `will determine the number of words in a term`(term: String, size: Int) {
    assertThat(TermSplitter.split(term).size).isEqualTo(size)
  }
}
