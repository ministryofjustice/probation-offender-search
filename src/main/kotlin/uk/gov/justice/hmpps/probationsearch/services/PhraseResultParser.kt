package uk.gov.justice.hmpps.probationsearch.services

import org.elasticsearch.search.SearchHit
import uk.gov.justice.hmpps.probationsearch.dto.IDs
import uk.gov.justice.hmpps.probationsearch.dto.OffenderDetail

internal fun extractOffenderDetailList(
  hits: Array<SearchHit>,
  phrase: String,
  offenderParser: (json: String) -> OffenderDetail,
  accessChecker: (offender: OffenderDetail) -> Boolean
): List<OffenderDetail> = hits
  .map { offenderParser(it.sourceAsString).mergeHighlights(it.highlightFields, phrase) }
  .map {
    if (accessChecker(it)) {
      it
    } else {
      redact(it)
    }
  }

fun redact(offenderDetail: OffenderDetail): OffenderDetail = OffenderDetail(
  accessDenied = true,
  offenderId = offenderDetail.offenderId,
  offenderManagers = offenderDetail.offenderManagers,
  otherIds = IDs(crn = offenderDetail.otherIds.crn)
)
