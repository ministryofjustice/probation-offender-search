package uk.gov.justice.hmpps.probationsearch.addresssearch

import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.InnerHitBuilder
import org.elasticsearch.index.query.Operator
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
  listOfNotNull(
    streetName.withBoost(boostOptions.streetName),
    town.withBoost(boostOptions.town),
    postcode.withBoost(boostOptions.postcode),
  ).sum()

private fun String?.withBoost(boost: Float) = if (isNullOrBlank()) 0.0F else boost

fun buildSearchQuery(addressSearchRequest: AddressSearchRequest): QueryBuilder =
  QueryBuilders.nestedQuery(
    "contactDetails.addresses",
    QueryBuilders.functionScoreQuery(matchQueryBuilder(addressSearchRequest))
      .setMinScore(addressSearchRequest.minBoost()),
    ScoreMode.Max,
  ).innerHit(InnerHitBuilder("addresses").setFetchSourceContext(FetchSourceContext(true)).setSize(100))

private fun matchQueryBuilder(addressSearchRequest: AddressSearchRequest) =
  QueryBuilders.boolQuery()
    .mustMatchNonNull(
      "contactDetails.addresses.streetName_analyzed",
      addressSearchRequest.streetName,
      addressSearchRequest.boostOptions.streetName,
      "streetName",
    )
    .mustMatchNonNull(
      "contactDetails.addresses.postcode_analyzed",
      addressSearchRequest.postcode,
      addressSearchRequest.boostOptions.postcode,
      "postcode",
    )
    .mustMatchNonNull(
      "contactDetails.addresses.town_analyzed",
      addressSearchRequest.town,
      addressSearchRequest.boostOptions.town,
      "town",
    )
    .mustMatchNonNull("contactDetails.addresses.addressNumber", addressSearchRequest.addressNumber, 1f)
    .mustMatchNonNull("contactDetails.addresses.district", addressSearchRequest.district, 1f)
    .mustMatchNonNull("contactDetails.addresses.town", addressSearchRequest.buildingName, 1f)
    .mustMatchNonNull("contactDetails.addresses.county", addressSearchRequest.county, 1f)
    .mustMatchNonNull("contactDetails.addresses.telephoneNumber", addressSearchRequest.telephoneNumber, 1f)

fun BoolQueryBuilder.mustMatchNonNull(
  name: String,
  value: String?,
  boost: Float,
  queryName: String? = null,
): BoolQueryBuilder {
  if (!value.isNullOrBlank()) {
    val qb = QueryBuilders.matchQuery(name, value).boost(boost).operator(Operator.AND)
    if (queryName != null) qb.queryName(queryName)
    must(qb)
  }
  return this
}
