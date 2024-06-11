package uk.gov.justice.hmpps.probationsearch.dto

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

class SearchPagedResults(
  content: Page<OffenderDetail>,
  pageable: Pageable,
  total: Long,
) : Paged<OffenderDetail>(
  content = content.content,
  total = total,
  totalPages = content.totalPages.toLong(),
  totalElements = content.totalElements,
  size = pageable.pageSize.toLong(),
  number = pageable.pageNumber.toLong(),
  numberOfElements = content.numberOfElements.toLong(),
  first = content.isFirst,
  last = content.isLast,
  empty = content.isEmpty,
  pageable = pageable,
  sort = content.sort,
)

