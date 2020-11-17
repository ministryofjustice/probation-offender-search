package uk.gov.justice.hmpps.offendersearch.services

import org.apache.lucene.search.join.ScoreMode.None
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.query.QueryBuilders.nestedQuery
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.bucket.nested.Nested
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import uk.gov.justice.hmpps.offendersearch.dto.ProbationAreaAggregation

const val offenderManagersAggregation = "offenderManagers"
const val activeOffenderManagerBucket = "active"
const val probationAreaCodeBucket = "byProbationAreaCode"

internal fun buildAggregationRequest(): NestedAggregationBuilder {
  return AggregationBuilders
    .nested(offenderManagersAggregation, "offenderManagers")
    .subAggregation(
      AggregationBuilders
        .terms(activeOffenderManagerBucket)
        .field("offenderManagers.active").subAggregation(
          AggregationBuilders
            .terms(probationAreaCodeBucket).size(1000)
            .field("offenderManagers.probationArea.code")
        )
    )
}

internal fun extractProbationAreaAggregation(aggregations: Aggregations): List<ProbationAreaAggregation> {
  val offenderManagersAggregation = aggregations.asMap()[offenderManagersAggregation] as Nested
  val activeAggregation = offenderManagersAggregation.aggregations.asMap()[activeOffenderManagerBucket] as Terms
  val possibleActiveBucket = activeAggregation.getBucketByKey("true")

  return possibleActiveBucket?.let { bucket ->
    val possibleProbationCodeBuckets = bucket.aggregations.asMap()[probationAreaCodeBucket] as Terms?
    possibleProbationCodeBuckets?.let { terms ->
      terms.buckets.map { ProbationAreaAggregation(code = it.keyAsString, count = it.docCount) }
    }
  } ?: listOf()
}

internal fun buildProbationAreaFilter(probationAreasCodes: List<String>): BoolQueryBuilder? {
  return probationAreasCodes
    .takeIf { it.isNotEmpty() }
    ?.let { probationAreaFilter(it) }
}

private fun probationAreaFilter(probationAreasCodes: List<String>): BoolQueryBuilder =
  QueryBuilders
    .boolQuery()
    .shouldAll(
      probationAreasCodes
        .map {
          nestedQuery(
            "offenderManagers",
            QueryBuilders.boolQuery().apply {
              must(QueryBuilders.termQuery("offenderManagers.active", true))
              must(QueryBuilders.termQuery("offenderManagers.probationArea.code", it))
            },
            None
          )
        }
    )
