package uk.gov.justice.hmpps.probationsearch.contactsearch.extensions

import org.opensearch.client.opensearch._types.OpenSearchException
import org.opensearch.client.opensearch._types.SortOptions
import org.opensearch.client.opensearch._types.SortOrder
import org.opensearch.client.opensearch._types.query_dsl.Query.Builder
import org.opensearch.client.opensearch.core.SearchRequest
import org.opensearch.client.opensearch.core.msearch.MultiSearchResponseItem
import org.opensearch.client.opensearch.core.msearch.MultisearchBody
import org.opensearch.client.opensearch.core.search.Hit
import org.opensearch.search.sort.SortBuilders
import org.opensearch.search.sort.SortOrder.DESC
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import uk.gov.justice.hmpps.probationsearch.contactsearch.extensions.OpenSearchRestClientExtensions.toSortOrder
import uk.gov.justice.hmpps.probationsearch.contactsearch.model.SortType
import uk.gov.justice.hmpps.probationsearch.contactsearch.model.SortType.LAST_UPDATED_DATETIME
import uk.gov.justice.hmpps.probationsearch.contactsearch.model.SortType.SCORE

object OpenSearchJavaClientExtensions {

  fun MultisearchBody.Builder.withPageable(pageable: Pageable): MultisearchBody.Builder = this
    .size(pageable.pageSize)
    .from(pageable.offset.toInt())
    .sort(buildSortOptions(pageable.sort))

  fun SearchRequest.Builder.withPageable(pageable: Pageable): SearchRequest.Builder = this
    .size(pageable.pageSize)
    .from(pageable.offset.toInt())
    .sort(buildSortOptions(pageable.sort))

  fun <T> MultiSearchResponseItem<T>.hits(): List<Hit<T>> =
    if (isFailure) throw OpenSearchException(failure()) else result().hits().hits()

  fun buildSortOptions(sort: Sort): List<SortOptions> {
    val fieldSorts = SortType.entries.flatMap { type ->
      type.aliases.mapNotNull { alias ->
        sort.getOrderFor(alias)?.let {
          SortBuilders.fieldSort(type.searchField).order(it.direction.toSortOrder())
        }
      }
    }.ifEmpty { listOf(SortBuilders.fieldSort(SCORE.searchField).order(DESC)) }
    return when (fieldSorts.size) {
      0 -> {
        listOf(
          SortOptions.Builder().field { f -> f.field(SCORE.searchField).order(SortOrder.Desc) }.build(),
          SortOptions.Builder()
            .field { f -> f.field(LAST_UPDATED_DATETIME.searchField).order(SortOrder.Desc) }.build(),
        )
      }

      else -> fieldSorts.map {
        SortOptions.Builder().field { f ->
          f.field(it.fieldName).order(if (it.order() == DESC) SortOrder.Desc else SortOrder.Asc)
        }.build()
      }
    }
  }

  fun Builder.matchesCrn(crn: String) =
    this.term { term -> term.field("crn").value { it.stringValue(crn) } }
}