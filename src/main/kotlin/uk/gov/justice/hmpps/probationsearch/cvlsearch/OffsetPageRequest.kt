package uk.gov.justice.hmpps.probationsearch.cvlsearch

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

class OffsetPageRequest(
  private val pageSize: Int = 100,
  private val offset: Int = 0,
  private val sort: Sort,
) : Pageable {

  override fun getPageNumber() = offset / pageSize
  override fun getPageSize() = pageSize
  override fun getOffset() = offset.toLong()
  override fun getSort() = sort

  override operator fun next(): Pageable {
    return OffsetPageRequest(offset + pageSize, pageSize, sort)
  }

  private fun previous(): OffsetPageRequest {
    return if (hasPrevious()) OffsetPageRequest(offset - pageSize, pageSize, sort) else this
  }

  override fun previousOrFirst(): Pageable {
    return if (hasPrevious()) previous() else first()
  }

  override fun first(): Pageable {
    return OffsetPageRequest(0, pageSize, sort)
  }

  override fun withPage(pageNumber: Int): Pageable {
    TODO("Not yet implemented")
  }

  override fun hasPrevious(): Boolean {
    return offset > pageSize
  }
}
