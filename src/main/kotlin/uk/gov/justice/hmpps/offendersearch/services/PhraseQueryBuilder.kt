package uk.gov.justice.hmpps.offendersearch.services

import org.elasticsearch.index.query.MultiMatchQueryBuilder.Type.CROSS_FIELDS
import org.elasticsearch.index.query.MultiMatchQueryBuilder.Type.MOST_FIELDS
import org.elasticsearch.index.query.Operator
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders

fun buildQuery(phrase: String, matchAllTerms: Boolean): QueryBuilder =

  if (matchAllTerms) {
    QueryBuilders.boolQuery()
        .mustIfPresent(maybeSimpleTermAndQuery(phrase))
        .mustAll(croNumberQueries(phrase))
        .mustAll(pncNumberQueries(phrase))
        .mustAll(dateQueries(phrase))
  } else {
    QueryBuilders.boolQuery()
        .shouldIfPresent(maybeSimpleTermOrQueryCrossFields(phrase))
        .shouldIfPresent(maybeSimpleTermOrQueryMostFields(phrase))
        .shouldAll(croNumberQueries(phrase))
        .shouldAll(pncNumberQueries(phrase))
        .shouldAll(dateQueries(phrase))
  }

private fun maybeSimpleTermAndQuery(phrase: String): QueryBuilder? =
  extractSearchableSimpleTerms(phrase)
      .takeIf { it.isNotEmpty() }
      ?.let {
        simpleTermAndQuery(it)
      }

private fun maybeSimpleTermOrQueryCrossFields(phrase: String): QueryBuilder? =
  extractSearchableSimpleTerms(phrase)
      .takeIf { it.isNotEmpty() }
      ?.let {
        simpleTermOrQueryCrossFields(it)
      }

private fun maybeSimpleTermOrQueryMostFields(phrase: String): QueryBuilder? =
  extractSearchableSimpleTerms(phrase)
      .takeIf { it.isNotEmpty() }
      ?.let {
        simpleTermOrQueryMostFields(it)
      }

private fun croNumberQueries(phrase: String): List<QueryBuilder> =
    extractCRONumberLikeTerms(phrase)
        .map { croNumberQuery(it) }

private fun pncNumberQueries(phrase: String): List<QueryBuilder> =
    extractPNCNumberLikeTerms(phrase)
        .map { pncNumberQuery(it) }

private fun dateQueries(phrase: String): List<QueryBuilder> =
    extractDateLikeTerms(phrase)
        .map { dateOfBirthQuery(it) }

private fun dateOfBirthQuery(phrase: String): QueryBuilder =
    QueryBuilders.matchQuery("dateOfBirth", phrase)
        .boost(11f)
        .lenient(true)

private fun pncNumberQuery(phrase: String): QueryBuilder =
    QueryBuilders.multiMatchQuery(phrase)
        .field("otherIds.pncNumberLongYear", 10f)
        .field("otherIds.pncNumberShortYear", 10f)
        .analyzer("whitespace")

private fun croNumberQuery(phrase: String): QueryBuilder =
    QueryBuilders.matchQuery("otherIds.croNumberLowercase", phrase)
        .boost(10f)
        .analyzer("whitespace")

private fun simpleTermAndQuery(phrase: String): QueryBuilder =
    QueryBuilders.multiMatchQuery(phrase)
        .field("firstName", 10f)
        .field("surname", 10f)
        .field("middleNames", 8f)
        .field("offenderAliases.firstName", 8f)
        .field("offenderAliases.surname", 8f)
        .field("contactDetails.addresses.town")
        .field("gender")
        .field("contactDetails.addresses.streetName")
        .field("contactDetails.addresses.county")
        .field("contactDetails.addresses.postcode", 10f)
        .field("otherIds.crn", 10f)
        .field("otherIds.nomsNumber", 10f)
        .field("otherIds.niNumber", 10f)
        .operator(Operator.AND)
        .type(CROSS_FIELDS)

private fun simpleTermOrQueryCrossFields(phrase: String): QueryBuilder =
    QueryBuilders.multiMatchQuery(phrase)
        .field("firstName", 10f)
        .field("surname", 10f)
        .field("middleNames", 8f)
        .field("offenderAliases.firstName", 8f)
        .field("offenderAliases.surname", 8f)
        .field("contactDetails.addresses.town")
        .type(CROSS_FIELDS)

private fun simpleTermOrQueryMostFields(phrase: String): QueryBuilder =
    QueryBuilders.multiMatchQuery(phrase)
        .field("gender")
        .field("otherIds.crn", 10f)
        .field("otherIds.nomsNumber", 10f)
        .field("otherIds.niNumber", 10f)
        .field("contactDetails.addresses.streetName")
        .field("contactDetails.addresses.county")
        .field("contactDetails.addresses.postcode", 10f)
        .type(MOST_FIELDS)
