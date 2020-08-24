package uk.gov.justice.hmpps.offendersearch.services

import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField
import uk.gov.justice.hmpps.offendersearch.dto.OffenderDetail
import java.time.format.DateTimeFormatter


internal fun buildHighlightRequest() = HighlightBuilder()
    .highlighterType("unified")
    .field("*")
    .preTags("")
    .postTags("")

internal fun OffenderDetail.mergeHighlights(highlightFields: Map<String, HighlightField>, phrase: String): OffenderDetail =
    this.copy(highlight = extractHighlights(highlightFields) + possibleDateOfBirthHighlight(phrase))


private fun OffenderDetail.possibleDateOfBirthHighlight(phrase: String): Map<String, List<String>> {
  val canonicalDateOfBirth = this.dateOfBirth?.format(DateTimeFormatter.ISO_DATE)
  return if (doAnyTermsMatchDateOfBirthValue(phrase, canonicalDateOfBirth)) {
    mapOf("dateOfBirth" to listOf(canonicalDateOfBirth!!))
  } else {
    mapOf()
  }
}

private fun doAnyTermsMatchDateOfBirthValue(phrase: String, dateOfBirth: String?) =
    extractDateLikeTerms(phrase).any { it == dateOfBirth }

fun extractHighlights(highlightFields: Map<String, HighlightField>): Map<String, List<String>> = highlightFields
    .toList()
    .map { it.first to it.second.fragments.map { fragment -> fragment.string() } }
    .toMap()
