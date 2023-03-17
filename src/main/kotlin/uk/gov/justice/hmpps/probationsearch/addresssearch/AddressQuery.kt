package uk.gov.justice.hmpps.probationsearch.addresssearch

import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.InnerHitBuilder
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.fetch.subphase.FetchSourceContext

fun matchAddresses(addressSearchRequest: AddressSearchRequest, maxResults: Int): SearchSourceBuilder =
  SearchSourceBuilder()
    .from(0)
    .size(maxResults).fetchSource(
      FetchSourceContext(
        true,
        arrayOf("offenderId", "gender", "otherIds.crn", "dateOfBirth"),
        arrayOf("contactDetails.addresses"),
      ),
    ).query(buildSearchQuery(addressSearchRequest))

private fun AddressSearchRequest.minBoost(): Float =
  listOf(boostOptions.streetName, boostOptions.postcode, boostOptions.buildingName).min()

fun buildSearchQuery(addressSearchRequest: AddressSearchRequest): QueryBuilder =
  QueryBuilders.nestedQuery(
    "contactDetails.addresses",
    QueryBuilders.functionScoreQuery(matchQueryBuilder(addressSearchRequest))
      .setMinScore(addressSearchRequest.minBoost()),
    ScoreMode.Max,
  ).innerHit(InnerHitBuilder("addresses").setFetchSourceContext(FetchSourceContext(true)).setSize(100))

private fun matchQueryBuilder(addressSearchRequest: AddressSearchRequest) =
  QueryBuilders.boolQuery()
    .shouldMatchNonNull(
      "contactDetails.addresses.streetName_analyzed",
      addressSearchRequest.streetName,
      addressSearchRequest.boostOptions.streetName,
      "streetName",
    )
    .shouldMatchNonNull(
      "contactDetails.addresses.postcode_analyzed",
      addressSearchRequest.postcode,
      addressSearchRequest.boostOptions.postcode,
      "postcode",
    )
    .shouldMatchNonNull(
      "contactDetails.addresses.buildingName",
      addressSearchRequest.buildingName,
      addressSearchRequest.boostOptions.buildingName,
      "buildingName",
    )
    .shouldMatchNonNull("contactDetails.addresses.addressNumber", addressSearchRequest.addressNumber, 1f)
    .shouldMatchNonNull("contactDetails.addresses.district", addressSearchRequest.district, 1f)
    .shouldMatchNonNull("contactDetails.addresses.town", addressSearchRequest.town, 1f)
    .shouldMatchNonNull("contactDetails.addresses.county", addressSearchRequest.county, 1f)
    .shouldMatchNonNull("contactDetails.addresses.telephoneNumber", addressSearchRequest.telephoneNumber, 1f)

fun BoolQueryBuilder.shouldMatchNonNull(
  name: String,
  value: String?,
  boost: Float,
  queryName: String? = null,
): BoolQueryBuilder {
  if (value != null && value.isNotBlank()) {
    val qb = QueryBuilders.matchQuery(name, value).boost(boost)
    if (queryName != null) qb.queryName(queryName)
    should(qb)
  }
  return this
}