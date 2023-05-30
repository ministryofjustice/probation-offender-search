package uk.gov.justice.hmpps.probationsearch.addresssearch

import org.apache.lucene.search.join.ScoreMode
import org.opensearch.index.query.BoolQueryBuilder
import org.opensearch.index.query.InnerHitBuilder
import org.opensearch.index.query.QueryBuilder
import org.opensearch.index.query.QueryBuilders
import org.opensearch.index.query.QueryBuilders.functionScoreQuery
import org.opensearch.index.query.QueryBuilders.nestedQuery
import org.opensearch.search.builder.SearchSourceBuilder
import org.opensearch.search.fetch.subphase.FetchSourceContext

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
  ).max()

private fun String?.withBoost(boost: Float) = if (isNullOrBlank()) 0.0F else boost

fun buildSearchQuery(addressSearchRequest: AddressSearchRequest) =
  nestedQuery(
    "contactDetails.addresses",
    functionScoreQuery(matchQueryBuilder(addressSearchRequest)).setMinScore(addressSearchRequest.minBoost()),
    ScoreMode.Max,
  ).innerHit(InnerHitBuilder("addresses").setFetchSourceContext(FetchSourceContext(true)).setSize(100))

private fun matchQueryBuilder(addressSearchRequest: AddressSearchRequest): QueryBuilder =
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
      "contactDetails.addresses.town_analyzed",
      addressSearchRequest.town,
      addressSearchRequest.boostOptions.town,
      "town",
    )
    .shouldMatchNonNull("contactDetails.addresses.addressNumber", addressSearchRequest.addressNumber, 1f)
    .shouldMatchNonNull("contactDetails.addresses.district", addressSearchRequest.district, 1f)
    .shouldMatchNonNull("contactDetails.addresses.buildingName", addressSearchRequest.buildingName, 1f)
    .shouldMatchNonNull("contactDetails.addresses.county", addressSearchRequest.county, 1f)
    .shouldMatchNonNull("contactDetails.addresses.telephoneNumber", addressSearchRequest.telephoneNumber, 1f)

fun BoolQueryBuilder.shouldMatchNonNull(
  name: String,
  value: String?,
  boost: Float,
  queryName: String? = null,
): BoolQueryBuilder {
  if (!value.isNullOrBlank()) {
    val qb = QueryBuilders.matchQuery(name, value).boost(boost)
    if (queryName != null) qb.queryName(queryName)
    should(qb)
  }
  return this
}
