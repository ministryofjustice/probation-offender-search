package uk.gov.justice.hmpps.probationsearch.cvlsearch

import org.apache.lucene.search.join.ScoreMode
import org.opensearch.index.query.QueryBuilder
import org.opensearch.index.query.QueryBuilders
import org.opensearch.search.builder.SearchSourceBuilder
import org.opensearch.search.fetch.subphase.FetchSourceContext
import org.opensearch.search.sort.FieldSortBuilder
import org.opensearch.search.sort.NestedSortBuilder
import org.opensearch.search.sort.SortBuilders
import org.opensearch.search.sort.SortOrder

class LicenceCaseloadQueryBuilder(private val request: LicenceCaseloadRequest) {
  val sourceBuilder: SearchSourceBuilder
    get() {
      val sb = SearchSourceBuilder()
        .from(request.offset)
        .size(request.pageSize)
        .fetchSource(
          FetchSourceContext(
            true,
            arrayOf("otherIds", "firstName", "middleNames", "surname", "offenderManagers"),
            arrayOf(),
          ),
        )

      if (request.query.isNotBlank()) {
        sb.query(QueryBuilders.boolQuery().must(searchQuery()).must(filterQuery()))
      } else {
        sb.query(filterQuery())
      }
      buildSort().forEach { sb.sort(it) }
      return sb
    }

  private fun filterQuery(): QueryBuilder =
    QueryBuilders.nestedQuery(
      "offenderManagers",
      QueryBuilders.boolQuery().filter(QueryBuilders.termsQuery("offenderManagers.team.code", request.teamCodes)),
      ScoreMode.None,
    )

  private fun searchQuery(): QueryBuilder =
    QueryBuilders.boolQuery()
      .should(
        QueryBuilders.wildcardQuery("otherIds.crn", "*${request.query.lowercase()}*"),
      )
      .should(
        QueryBuilders.wildcardQuery("otherIds.previousCrn", "*${request.query.lowercase()}*"),
      )
      .should(
        QueryBuilders.wildcardQuery("firstName", "*${request.query.lowercase()}*"),
      )
      .should(
        QueryBuilders.wildcardQuery("surname", "*${request.query.lowercase()}*"),
      )
      .should(
        QueryBuilders.wildcardQuery("middleNames", "*${request.query.lowercase()}*"),
      )
      .should(
        QueryBuilders.nestedQuery(
          "offenderManagers",
          QueryBuilders.wildcardQuery("offenderManagers.staff.surname", "*${request.query.lowercase()}*"),
          ScoreMode.None,
        ),
      )
      .should(
        QueryBuilders.nestedQuery(
          "offenderManagers",
          QueryBuilders.wildcardQuery("offenderManagers.staff.forenames", "*${request.query.lowercase()}*"),
          ScoreMode.None,
        ),
      )

  private fun buildSort(): List<FieldSortBuilder> {
    return request.sortBy.map {
      val sb = SortBuilders.fieldSort(SortField.fromInput(it.field)?.searchField)
        .order(SortOrder.valueOf(it.direction.uppercase()))
      if (it.field.startsWith("manager")) sb.nestedSort = NestedSortBuilder("offenderManagers")
      sb
    }
  }
}
