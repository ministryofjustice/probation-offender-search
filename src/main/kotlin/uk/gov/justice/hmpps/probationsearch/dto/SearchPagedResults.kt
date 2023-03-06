package uk.gov.justice.hmpps.probationsearch.dto

import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

class SearchPagedResults(
  content: List<OffenderDetail>,
  pageable: Pageable,
  total: Long,
) : PageImpl<OffenderDetail>(content, pageable, total)
