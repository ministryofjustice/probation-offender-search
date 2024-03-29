package uk.gov.justice.hmpps.probationsearch.services

import org.apache.lucene.search.join.ScoreMode.None
import org.opensearch.index.query.BoolQueryBuilder
import org.opensearch.index.query.QueryBuilders
import org.opensearch.index.query.QueryBuilders.nestedQuery
import org.opensearch.search.aggregations.AggregationBuilders
import org.opensearch.search.aggregations.Aggregations
import org.opensearch.search.aggregations.bucket.nested.Nested
import org.opensearch.search.aggregations.bucket.nested.NestedAggregationBuilder
import org.opensearch.search.aggregations.bucket.terms.Terms
import org.opensearch.search.aggregations.metrics.TopHits
import uk.gov.justice.hmpps.probationsearch.dto.ProbationAreaAggregation

const val offenderManagersAggregation = "offenderManagers"
const val activeOffenderManagerBucket = "active"
const val probationAreaCodeBucket = "byProbationAreaCode"
const val probationAreaDescriptionBucket = "probationAreaDescription"

internal fun buildAggregationRequest(): NestedAggregationBuilder {
  return AggregationBuilders
    .nested(offenderManagersAggregation, "offenderManagers")
    .subAggregation(
      AggregationBuilders
        .terms(activeOffenderManagerBucket)
        .field("offenderManagers.active").subAggregation(
          AggregationBuilders
            .terms(probationAreaCodeBucket).size(1000)
            .field("offenderManagers.probationArea.code").subAggregation(
              AggregationBuilders
                .topHits(probationAreaDescriptionBucket)
                .fetchSource(arrayOf("offenderManagers.probationArea.description"), emptyArray())
                .size(1),
            ),
        ),
    )
}

internal fun extractProbationAreaAggregation(aggregations: Aggregations): List<ProbationAreaAggregation> {
  val offenderManagersAggregation = aggregations.asMap()[offenderManagersAggregation] as Nested
  val activeAggregation = offenderManagersAggregation.aggregations.asMap()[activeOffenderManagerBucket] as Terms
  val possibleActiveBucket = activeAggregation.getBucketByKey("true")

  return possibleActiveBucket?.let { bucket ->
    val possibleProbationCodeBuckets = bucket.aggregations.asMap()[probationAreaCodeBucket] as Terms?
    possibleProbationCodeBuckets?.let { terms ->
      terms.buckets.map {
        val topHit = (it.aggregations.asMap()[probationAreaDescriptionBucket] as TopHits).hits.hits[0]
        val description = (topHit.sourceAsMap["probationArea"] as Map<*, *>)["description"] as String
        ProbationAreaAggregation(code = it.keyAsString, description = description, count = it.docCount)
      }
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
            None,
          )
        },
    )
