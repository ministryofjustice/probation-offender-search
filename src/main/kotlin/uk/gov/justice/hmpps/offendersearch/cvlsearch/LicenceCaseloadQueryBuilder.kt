package uk.gov.justice.hmpps.offendersearch.cvlsearch

import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.fetch.subphase.FetchSourceContext
import org.elasticsearch.search.sort.FieldSortBuilder
import org.elasticsearch.search.sort.NestedSortBuilder
import org.elasticsearch.search.sort.SortBuilders
import org.elasticsearch.search.sort.SortOrder

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
            arrayOf()
          )
        )

      if (request.query.isNotBlank()) sb.query(QueryBuilders.boolQuery().must(searchQuery()).must(filterQuery()))
      else sb.query(filterQuery())
      return sb.sort(buildSort())
    }

  private fun filterQuery(): QueryBuilder =
    QueryBuilders.nestedQuery(
      "offenderManagers",
      QueryBuilders.boolQuery().filter(QueryBuilders.termsQuery("offenderManagers.team.code", request.teamCodes)),
      ScoreMode.None
    )

  private fun searchQuery(): QueryBuilder =
    QueryBuilders.boolQuery()
      .should(
        QueryBuilders.wildcardQuery("otherIds.crn", "*${request.query.lowercase()}*")
      )
      .should(
        QueryBuilders.wildcardQuery("otherIds.previousCrn", "*${request.query.lowercase()}*")
      )
      .should(
        QueryBuilders.wildcardQuery("firstName", "*${request.query.lowercase()}*")
      )
      .should(
        QueryBuilders.wildcardQuery("surname", "*${request.query.lowercase()}*")
      )
      .should(
        QueryBuilders.wildcardQuery("middleNames", "*${request.query.lowercase()}*")
      )
      .should(
        QueryBuilders.nestedQuery(
          "offenderManagers",
          QueryBuilders.wildcardQuery("offenderManagers.staff.surname", "*${request.query.lowercase()}*"),
          ScoreMode.None
        )
      )
      .should(
        QueryBuilders.nestedQuery(
          "offenderManagers",
          QueryBuilders.wildcardQuery("offenderManagers.staff.forenames", "*${request.query.lowercase()}*"),
          ScoreMode.None
        )
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
