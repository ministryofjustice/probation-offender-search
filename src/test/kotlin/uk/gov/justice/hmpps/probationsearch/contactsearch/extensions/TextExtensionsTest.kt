package uk.gov.justice.hmpps.probationsearch.contactsearch.extensions

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.hmpps.probationsearch.contactsearch.extensions.TextExtensions.asHighlightedFragmentOf
import uk.gov.justice.hmpps.probationsearch.contactsearch.extensions.TextExtensions.asTextChunks
import java.util.stream.Stream

internal class TextExtensionsTest {
  companion object {
    @JvmStatic
    fun chunkCases(): Stream<Arguments> = Stream.of(
      arguments("a b c", 4, 0, listOf("a b c")),
      arguments("a b c d", 4, 0, listOf("a b c d")),
      arguments("a b c d", 4, 2, listOf("a b c d", "c d")),
      arguments(
        "one two three four five six seven eight", 4, 1,
        listOf(
          "one two three four",
          "four five six seven",
          "seven eight",
        ),
      ),
      arguments(
        (1..65).joinToString(" ") { "token$it" }, 32, 4,
        listOf(
          (1..32).joinToString(" ") { "token$it" },
          (29..60).joinToString(" ") { "token$it" },
          (57..65).joinToString(" ") { "token$it" },
        ),
      ),
      arguments(
        "hello!!!!!! --- world, this;is a. test", 2, 0,
        listOf(
          "hello  world",
          "this;is",
          "a. test",
        ),
      ),
    )

    @JvmStatic
    fun highlightCases(): Stream<Arguments> = Stream.of(
      arguments("No match here.", "missing", 400, "<em>missing</em>"),
      arguments(
        "First sentence. Second sentence with important chunk. Third sentence.",
        "important chunk",
        400,
        "Second sentence with <em>important chunk</em>.",
      ),
      arguments(
        "First sentence. Second sentence with important chunk. Third sentence.",
        "sentence. Second sentence with important",
        400,
        "First <em>sentence. Second sentence with important</em> chunk.",
      ),
      arguments(
        "A long sentence that contains an important chunk somewhere in the middle of the sentence but the sentence is longer than max fragment size.",
        "important chunk",
        30,
        "...<em>important chunk</em>...",
      ),
      arguments(
        "A long sentence that contains an important chunk somewhere in the middle of the sentence but the sentence is longer than max fragment size.",
        "important chunk",
        1,
        "...<em>important chunk</em>...",
      ),
    )
  }

  @ParameterizedTest
  @MethodSource("chunkCases")
  fun `asTextChunks should produce expected chunks`(
    input: String,
    chunkSize: Int,
    overlap: Int,
    expected: List<String>,
  ) {
    val result = input.asTextChunks(chunkSize, overlap).toList()
    assertThat(result).isEqualTo(expected)
  }

  @ParameterizedTest
  @MethodSource("highlightCases")
  fun `highlightedFragment should highlight text chunk within sentence`(
    input: String,
    chunk: String,
    fragmentSize: Int,
    expected: String,
  ) {
    val result = chunk.asHighlightedFragmentOf(input, fragmentSize)
    assertThat(result).isEqualTo(expected)
  }
}