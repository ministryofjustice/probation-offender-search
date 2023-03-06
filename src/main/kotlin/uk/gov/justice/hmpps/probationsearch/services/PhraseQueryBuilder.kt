package uk.gov.justice.hmpps.probationsearch.services

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
      .mustAll(phoneQueries(phrase))
  } else {
    QueryBuilders.boolQuery()
      .shouldIfPresent(maybeSimpleTermOrQueryCrossFields(phrase))
      .shouldIfPresent(maybeSimpleTermOrQueryMostFields(phrase))
      .shouldAll(croNumberQueries(phrase))
      .shouldAll(pncNumberQueries(phrase))
      .shouldAll(dateQueries(phrase))
      .shouldAll(simpleTermsWithSingleLetters(phrase))
      .shouldAll(phoneQueries(phrase))
  }.mustNot(QueryBuilders.termQuery("softDeleted", true))

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

private fun simpleTermsWithSingleLetters(phrase: String): List<QueryBuilder> =
  extractSearchableSimpleTermsWithSingleLetters(phrase)
    .map { simpleTermsPrefixQuery(it) }

private fun croNumberQueries(phrase: String): List<QueryBuilder> =
  extractCRONumberLikeTerms(phrase)
    .map { croNumberQuery(it) }

private fun pncNumberQueries(phrase: String): List<QueryBuilder> =
  extractPNCNumberLikeTerms(phrase)
    .map { pncNumberQuery(it) }

private fun dateQueries(phrase: String): List<QueryBuilder> =
  extractDateLikeTerms(phrase)
    .map { dateOfBirthQuery(it) }

private fun dateOfBirthQuery(term: String): QueryBuilder =
  QueryBuilders.matchQuery("dateOfBirth", term)
    .boost(11f)
    .lenient(true)

private fun phoneQueries(phrase: String): List<QueryBuilder> =
  extractPhoneNumberLikeTerms(phrase)
    .map { phoneNumberQuery(it) }
private fun phoneNumberQuery(term: String): QueryBuilder =
  QueryBuilders.matchQuery("contactNumbers", term)
    .boost(10f)
    .lenient(true)
    .analyzer("whitespace")

private fun pncNumberQuery(term: String): QueryBuilder =
  QueryBuilders.multiMatchQuery(term)
    .field("otherIds.pncNumberLongYear", 10f)
    .field("otherIds.pncNumberShortYear", 10f)
    .analyzer("whitespace")

private fun croNumberQuery(term: String): QueryBuilder =
  QueryBuilders.matchQuery("otherIds.croNumberLowercase", term)
    .boost(10f)
    .analyzer("whitespace")

private fun simpleTermAndQuery(phrase: String): QueryBuilder =
  QueryBuilders.multiMatchQuery(phrase).analyzer("standard")
    .field("firstName", 10f)
    .field("surname", 10f)
    .field("middleNames", 8f)
    .field("offenderAliases.firstName", 1.5f)
    .field("offenderAliases.surname", 1.5f)
    .field("contactDetails.addresses.town")
    .field("contactNumbers")
    .field("gender")
    .field("contactDetails.addresses.streetName")
    .field("contactDetails.addresses.county")
    .field("contactDetails.addresses.postcode", 10f)
    .field("otherIds.crn", 10f)
    .field("otherIds.previousCrn", 10f)
    .field("otherIds.nomsNumber", 10f)
    .field("otherIds.niNumber", 10f)
    .operator(Operator.AND)
    .type(CROSS_FIELDS)

private fun simpleTermOrQueryCrossFields(phrase: String): QueryBuilder =
  QueryBuilders.multiMatchQuery(phrase).analyzer("standard")
    .field("firstName", 10f)
    .field("surname", 10f)
    .field("middleNames", 8f)
    .field("offenderAliases.firstName", 1.5f)
    .field("offenderAliases.surname", 1.5f)
    .field("contactDetails.addresses.town")
    .field("contactNumbers")
    .operator(Operator.OR)
    .type(CROSS_FIELDS)

private fun simpleTermOrQueryMostFields(phrase: String): QueryBuilder =
  QueryBuilders.multiMatchQuery(phrase).analyzer("standard")
    .field("gender")
    .field("otherIds.crn", 10f)
    .field("otherIds.previousCrn", 10f)
    .field("otherIds.nomsNumber", 10f)
    .field("otherIds.niNumber", 10f)
    .field("contactDetails.addresses.streetName")
    .field("contactDetails.addresses.county")
    .field("contactDetails.addresses.postcode", 10f)
    .field("contactNumbers")
    .operator(Operator.OR)
    .type(MOST_FIELDS)

fun simpleTermsPrefixQuery(term: String): QueryBuilder =
  QueryBuilders.prefixQuery("firstName", term.lowercase()).boost(11f)
