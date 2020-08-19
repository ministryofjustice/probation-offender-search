package uk.gov.justice.hmpps.offendersearch.services

import org.elasticsearch.index.query.MultiMatchQueryBuilder
import org.elasticsearch.index.query.Operator
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders

fun buildQuery(phrase: String, matchAllTerms: Boolean): QueryBuilder =
  QueryBuilders.boolQuery()
      .mustIfPresent(maybeSimpleTermQuery(phrase))
      .mustAll(CRONumberQueries(phrase))
      .mustAll(PNCNumberQueries(phrase))
      .mustAll(dateQueries(phrase))

fun maybeSimpleTermQuery(phrase: String): QueryBuilder? =
  extractSearchableSimpleTerms(phrase)
      .takeIf { it.isNotEmpty() }
      ?.let {
        simpleTermQuery(it)
      }

private fun CRONumberQueries(phrase: String): List<QueryBuilder> =
    extractCRONumberLikeTerms(phrase)
        .map { croNumberQuery(it) }

private fun PNCNumberQueries(phrase: String): List<QueryBuilder> =
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

private fun simpleTermQuery(phrase: String): QueryBuilder =
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
        .type(MultiMatchQueryBuilder.Type.CROSS_FIELDS)