package uk.gov.justice.hmpps.probationsearch.contactsearch.extensions

import org.apache.lucene.analysis.standard.StandardTokenizer
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute
import java.io.StringReader
import java.text.BreakIterator

object TextExtensions {
  fun String.asTextChunks(chunkSize: Int = 32, overlap: Int = 4): Sequence<String> {
    val sanitizedText = this
      .replace("(^|\\s)[^A-Za-z0-9\\s]{2,}(\\s|$)".toRegex(), " ")
      .replace("[^A-Za-z0-9\\s]{4,}".toRegex(), " ")
    val offsets = sanitizedText.tokenOffsets()
    return offsets.windowed(size = chunkSize, step = chunkSize - overlap, partialWindows = true) { window ->
      sanitizedText.substring(window.first().first, window.last().second)
    }
  }

  fun String.tokenOffsets(): Sequence<Pair<Int, Int>> {
    val tokenizer = StandardTokenizer()
    tokenizer.setReader(StringReader(this@tokenOffsets))
    tokenizer.reset()
    return sequence {
      while (tokenizer.incrementToken()) {
        val offset = tokenizer.getAttribute(OffsetAttribute::class.java)
        yield(offset.startOffset() to offset.endOffset())
      }
      tokenizer.close()
    }
  }

  fun String.asHighlightedFragmentOf(
    fullText: String,
    maxFragmentSize: Int = 600,
    preTag: String = "<em>",
    postTag: String = "</em>",
  ): String {
    val chunkStart = fullText.indexOf(this).takeIf { it >= 0 } ?: return "$preTag$this$postTag"
    val chunkEnd = chunkStart + length

    val breakIterator = BreakIterator.getSentenceInstance().apply { setText(fullText) }
    val sentenceStart = breakIterator.preceding(chunkStart + 1).takeIf { it != BreakIterator.DONE } ?: 0
    val sentenceEnd = breakIterator.following(chunkEnd - 1).takeIf { it != BreakIterator.DONE } ?: fullText.length

    val sentence = fullText.substring(sentenceStart, sentenceEnd).trim()
    val fragment = if (sentence.length <= maxFragmentSize) sentence else "...$this..."

    return fragment.replaceFirst(this, "$preTag$this$postTag")
  }
}