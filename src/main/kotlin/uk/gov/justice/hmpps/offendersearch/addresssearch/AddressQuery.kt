package uk.gov.justice.hmpps.offendersearch.addresssearch

import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.common.lucene.search.function.CombineFunction
import org.elasticsearch.common.lucene.search.function.FunctionScoreQuery
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.InnerHitBuilder
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.fetch.subphase.FetchSourceContext

fun matchAddresses(addressSearchRequest: AddressSearchRequest): SearchSourceBuilder = SearchSourceBuilder()
  .from(0)
  .size(10).fetchSource(
    FetchSourceContext(
      true,
      arrayOf("offenderId", "gender", "otherIds.crn", "dateOfBirth"),
      arrayOf("contactDetails.addresses")
    )
  )
  .query(buildSearchQuery(addressSearchRequest))

fun buildSearchQuery(addressSearchRequest: AddressSearchRequest): QueryBuilder =
  QueryBuilders.nestedQuery(
    "contactDetails.addresses",
    QueryBuilders.functionScoreQuery(matchQueryBuilder(addressSearchRequest))
      .scoreMode(FunctionScoreQuery.ScoreMode.SUM)
      .boostMode(CombineFunction.SUM)
      .setMinScore(5f),
    ScoreMode.Total
  )
    .innerHit(InnerHitBuilder("addresses").setFetchSourceContext(FetchSourceContext(true)))

private fun matchQueryBuilder(addressSearchRequest: AddressSearchRequest) =
  QueryBuilders.boolQuery()
    .shouldMatchNonNull("contactDetails.addresses.streetName", addressSearchRequest.streetName, 10f)
    .shouldMatchNonNull("contactDetails.addresses.postCode", addressSearchRequest.postcode, 10f)
    .shouldMatchNonNull("contactDetails.addresses.buildingName", addressSearchRequest.buildingName, 5f)
    .shouldMatchNonNull("contactDetails.addresses.addressNumber", addressSearchRequest.addressNumber, 1f)
    .shouldMatchNonNull("contactDetails.addresses.district", addressSearchRequest.district, 1f)
    .shouldMatchNonNull("contactDetails.addresses.town", addressSearchRequest.town, 1f)
    .shouldMatchNonNull("contactDetails.addresses.county", addressSearchRequest.county, 1f)
    .shouldMatchNonNull("contactDetails.addresses.telephoneNumber", addressSearchRequest.telephoneNumber, 1f)
    .should(QueryBuilders.rangeQuery("contactDetails.addresses.to").gte("now"))
    .mustNot(QueryBuilders.existsQuery("contactDetails.addresses.to"))

fun BoolQueryBuilder.shouldMatchNonNull(name: String, value: Any?, boost: Float): BoolQueryBuilder {
  if (value != null) {
    should(QueryBuilders.matchQuery(name, value).boost(boost))
  }
  return this
}
