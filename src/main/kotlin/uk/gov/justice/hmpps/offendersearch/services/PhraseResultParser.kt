package uk.gov.justice.hmpps.offendersearch.services

import org.elasticsearch.search.SearchHit
import uk.gov.justice.hmpps.offendersearch.dto.OffenderDetail


internal fun extractOffenderDetailList(
    hits: Array<SearchHit>,
    phrase: String,
    offenderParser: (json: String) -> OffenderDetail
): List<OffenderDetail> {
  return hits.map { offenderParser(it.sourceAsString).mergeHighlights(it.highlightFields, phrase) }
}