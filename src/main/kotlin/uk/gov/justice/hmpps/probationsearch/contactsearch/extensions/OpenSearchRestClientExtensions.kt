package uk.gov.justice.hmpps.probationsearch.contactsearch.extensions

import org.opensearch.data.client.orhlc.NativeSearchQuery
import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder
import org.opensearch.search.sort.FieldSortBuilder
import org.opensearch.search.sort.SortBuilders
import org.opensearch.search.sort.SortOrder.ASC
import org.opensearch.search.sort.SortOrder.DESC
import org.springframework.data.domain.Sort
import uk.gov.justice.hmpps.probationsearch.contactsearch.model.SortType
import uk.gov.justice.hmpps.probationsearch.contactsearch.model.SortType.LAST_UPDATED_DATETIME
import uk.gov.justice.hmpps.probationsearch.contactsearch.model.SortType.SCORE

object OpenSearchRestClientExtensions {
  fun Sort.Direction.toSortOrder() = when (this) {
    Sort.Direction.ASC -> ASC
    Sort.Direction.DESC -> DESC
  }

  fun Sort.fieldSorts() = SortType.entries.flatMap { type ->
    type.aliases.mapNotNull { alias ->
      getOrderFor(alias)?.let {
        SortBuilders.fieldSort(type.searchField).order(it.direction.toSortOrder())
      }
    }
  }.ifEmpty { listOf(SortBuilders.fieldSort(SCORE.searchField).order(DESC)) }

  fun NativeSearchQueryBuilder.sorted(sorts: List<FieldSortBuilder>): NativeSearchQuery {
    sorted(sorts) { this.withSorts(it) }
    return this.build()
  }

  fun sorted(sorts: List<FieldSortBuilder>, sortFn: (List<FieldSortBuilder>) -> Unit) {
    when (sorts.size) {
      0 -> {
        sortFn(
          listOf(
            SortBuilders.fieldSort(SCORE.searchField).order(DESC),
            SortBuilders.fieldSort(LAST_UPDATED_DATETIME.searchField).order(DESC),
          ),
        )
      }

      1 -> {
        val sorted = sorts.first()
        sortFn(
          listOf(
            sorted,
            when (sorted.fieldName) {
              in SCORE.aliases -> SortBuilders.fieldSort(LAST_UPDATED_DATETIME.searchField).order(sorted.order())
              else -> SortBuilders.fieldSort(SCORE.searchField).order(sorted.order())
            },
          ),
        )
      }

      else -> sortFn(sorts)
    }
  }
}