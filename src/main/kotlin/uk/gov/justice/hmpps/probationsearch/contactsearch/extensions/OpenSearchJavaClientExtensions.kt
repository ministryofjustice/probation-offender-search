package uk.gov.justice.hmpps.probationsearch.contactsearch.extensions

import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.OpenSearchException
import org.opensearch.client.opensearch._types.SortOptions
import org.opensearch.client.opensearch._types.SortOrder
import org.opensearch.client.opensearch._types.query_dsl.Query
import org.opensearch.client.opensearch._types.query_dsl.Query.Builder
import org.opensearch.client.opensearch.core.SearchRequest
import org.opensearch.client.opensearch.core.SearchResponse
import org.opensearch.client.opensearch.core.msearch.MultiSearchResponseItem
import org.opensearch.client.opensearch.core.search.Hit
import org.opensearch.client.util.ObjectBuilder
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import uk.gov.justice.hmpps.probationsearch.contactsearch.model.SortType
import uk.gov.justice.hmpps.probationsearch.contactsearch.model.SortType.SCORE
import java.util.function.Function

object OpenSearchJavaClientExtensions {
  inline fun <reified TDocument> OpenSearchClient.search(fn: Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>): SearchResponse<TDocument> =
    search<TDocument>(fn, TDocument::class.java)

  fun Sort.Direction.toSortOrder() = when (this) {
    Sort.Direction.ASC -> SortOrder.Asc
    Sort.Direction.DESC -> SortOrder.Desc
  }

  fun SearchRequest.Builder.withPageable(pageable: Pageable): SearchRequest.Builder = this
    .size(pageable.pageSize)
    .from(pageable.offset.toInt())
    .sort(buildSortOptions(pageable.sort))

  fun <T> MultiSearchResponseItem<T>.hits(): List<Hit<T>> =
    if (isFailure) throw OpenSearchException(failure()) else result().hits().hits()

  fun buildSortOptions(sort: Sort): List<SortOptions> {
    return SortType.entries.flatMap { type ->
      type.aliases.mapNotNull { alias ->
        sort.getOrderFor(alias)?.let { order ->
          SortOptions.Builder().field { it.field(type.searchField).order(order.direction.toSortOrder()) }.build()
        }
      }
    }.ifEmpty {
      listOf(
        SortOptions.Builder().field { it.field(SCORE.searchField).order(SortOrder.Desc) }.build(),
      )
    }
  }

  fun Builder.matchesCrn(crn: String): ObjectBuilder<Query> =
    this.term { term -> term.field("crn").value { it.stringValue(crn) } }
}