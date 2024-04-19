package uk.gov.justice.hmpps.probationsearch.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.opensearch.common.xcontent.XContentType.JSON
import org.opensearch.core.ParseField
import org.opensearch.core.xcontent.ContextParser
import org.opensearch.core.xcontent.DeprecationHandler
import org.opensearch.core.xcontent.NamedXContentRegistry
import org.opensearch.core.xcontent.NamedXContentRegistry.Entry
import org.opensearch.core.xcontent.XContentParser
import org.opensearch.index.query.BoolQueryBuilder
import org.opensearch.index.query.NestedQueryBuilder
import org.opensearch.index.query.QueryBuilders
import org.opensearch.search.aggregations.Aggregation
import org.opensearch.search.aggregations.Aggregations
import org.opensearch.search.aggregations.bucket.nested.NestedAggregationBuilder
import org.opensearch.search.aggregations.bucket.nested.ParsedNested
import org.opensearch.search.aggregations.bucket.terms.LongTerms
import org.opensearch.search.aggregations.bucket.terms.ParsedLongTerms
import org.opensearch.search.aggregations.bucket.terms.ParsedStringTerms
import org.opensearch.search.aggregations.bucket.terms.StringTerms
import org.opensearch.search.aggregations.bucket.terms.TermsAggregationBuilder
import org.opensearch.search.aggregations.metrics.ParsedTopHits
import org.opensearch.search.aggregations.metrics.TopHitsAggregationBuilder
import uk.gov.justice.hmpps.probationsearch.dto.ProbationAreaAggregation

internal class ProbationAreaFilterAggregatorKtTest {
  @Nested
  inner class BuildAggregationRequest {
    private val aggregation = buildAggregationRequest()

    @Test
    internal fun `will request buckets for active and inactive offender managers`() {
      assertThat(aggregation.subAggregations).hasSize(1)
      val activeOffenderManagerAggregation = aggregation.subAggregations.first() as TermsAggregationBuilder
      assertThat(activeOffenderManagerAggregation.field()).isEqualTo("offenderManagers.active")
      assertThat(activeOffenderManagerAggregation.name).isEqualTo("active")
    }

    @Test
    internal fun `will divide active and inactive offender managers into probation area buckets`() {
      assertThat(aggregation.subAggregations.first().subAggregations).hasSize(1)
      val probationAreaCodeAggregation =
        aggregation.subAggregations.first().subAggregations.first() as TermsAggregationBuilder
      assertThat(probationAreaCodeAggregation.field()).isEqualTo("offenderManagers.probationArea.code")
      assertThat(probationAreaCodeAggregation.name).isEqualTo("byProbationAreaCode")
    }

    @Test
    internal fun `will restrict probation area bucket to more than know number of areas`() {
      val probationAreaCodeAggregation =
        aggregation.subAggregations.first().subAggregations.first() as TermsAggregationBuilder
      assertThat(probationAreaCodeAggregation.size()).isEqualTo(1000)
    }
  }
}

internal class ExtractProbationAreaAggregation {
  @Test
  internal fun `will return no aggregations when empty`() {
    val aggregations =
      """
          { 
              "nested#offenderManagers": {
                "doc_count":0,
                "lterms#active": {
                  "doc_count_error_upper_bound":0,
                  "sum_other_doc_count":0,
                  "buckets":[]
                }
              }
          }""".toAggregation()
    assertThat(extractProbationAreaAggregation(aggregations)).isEmpty()
  }

  @Test
  internal fun `will only return action manager areas aggregations when found`() {
    val aggregations =
      """
          {
            "nested#offenderManagers": {
              "doc_count": 12,
              "lterms#active": {
                "doc_count_error_upper_bound": 0,
                "sum_other_doc_count": 0,
                "buckets": [
                  {
                    "key": 0,
                    "key_as_string": "false",
                    "doc_count": 6,
                    "sterms#byProbationAreaCode": {
                      "doc_count_error_upper_bound": 0,
                      "sum_other_doc_count": 0,
                      "buckets": [
                        {
                          "key": "N01",
                          "doc_count": 1,
                          "top_hits#probationAreaDescription": {
                            "hits": {
                              "hits": [
                                {
                                  "_source": {
                                    "probationArea": {
                                      "description": "North East"
                                    }
                                  }
                                }
                              ]
                            }
                          }
                        }
                      ]
                    }
                  },
                  {
                    "key": 1,
                    "key_as_string": "true",
                    "doc_count": 6,
                    "sterms#byProbationAreaCode": {
                      "doc_count_error_upper_bound": 0,
                      "sum_other_doc_count": 0,
                      "buckets": [
                        {
                          "key": "N01",
                          "doc_count": 3,
                          "top_hits#probationAreaDescription": {
                            "hits": {
                              "hits": [
                                {
                                  "_source": {
                                    "probationArea": {
                                      "description": "North West"
                                    }
                                  }
                                }
                              ]
                            }
                          }
                        },
                        {
                          "key": "N07",
                          "doc_count": 2,
                          "top_hits#probationAreaDescription": {
                            "hits": {
                              "hits": [
                                {
                                  "_source": {
                                    "probationArea": {
                                      "description": "London"
                                    }
                                  }
                                }
                              ]
                            }
                          }
                        },
                        {
                          "key": "N02",
                          "doc_count": 1,
                          "top_hits#probationAreaDescription": {
                            "hits": {
                              "hits": [
                                {
                                  "_source": {
                                    "probationArea": {
                                      "description": "North East"
                                    }
                                  }
                                }
                              ]
                            }
                          }
                        }
                      ]
                    }
                  }
                ]
              }
            }
          }""".toAggregation()
    assertThat(extractProbationAreaAggregation(aggregations)).containsExactlyInAnyOrder(
      ProbationAreaAggregation("N01", "North West", 3),
      ProbationAreaAggregation("N07", "London", 2),
      ProbationAreaAggregation("N02", "North East", 1),
    )
  }

  @Test
  internal fun `will return no aggregations when no active managers found`() {
    val aggregations =
      """
          {
            "nested#offenderManagers": {
              "doc_count": 12,
              "lterms#active": {
                "doc_count_error_upper_bound": 0,
                "sum_other_doc_count": 0,
                "buckets": [
                  {
                    "key": 0,
                    "key_as_string": "false",
                    "doc_count": 6,
                    "sterms#byProbationAreaCode": {
                      "doc_count_error_upper_bound": 0,
                      "sum_other_doc_count": 0,
                      "buckets": [
                        {
                          "key": "N01",
                          "doc_count": 1,
                          "top_hits#probationAreaDescription": {
                            "hits": {
                              "hits": [
                                {
                                  "_source": {
                                    "probationArea": {
                                      "description": "North West"
                                    }
                                  }
                                }
                              ]
                            }
                          }
                        }
                      ]
                    }
                  }
                ]
              }
            }
          }""".toAggregation()
    assertThat(extractProbationAreaAggregation(aggregations)).isEmpty()
  }

  @Test
  internal fun `will return no aggregations when no probation area codes found`() {
    val aggregations =
      """
          {
            "nested#offenderManagers": {
              "doc_count": 12,
              "lterms#active": {
                "doc_count_error_upper_bound": 0,
                "sum_other_doc_count": 0,
                "buckets": [
                  {
                    "key": 0,
                    "key_as_string": "true",
                    "doc_count": 6,
                    "sterms#someOtherBucket": {
                      "doc_count_error_upper_bound": 0,
                      "sum_other_doc_count": 0,
                      "buckets": [
                        {
                          "key": "N01",
                          "doc_count": 1,
                          "top_hits#probationAreaDescription": {
                            "hits": {
                              "hits": [
                                {
                                  "_source": {
                                    "probationArea": {
                                      "description": "North West"
                                    }
                                  }
                                }
                              ]
                            }
                          }
                        }
                      ]
                    }
                  }
                ]
              }
            }
          }""".toAggregation()
    assertThat(extractProbationAreaAggregation(aggregations)).isEmpty()
  }
}

internal class BuildProbationAreaFilter {
  @Test
  internal fun `will return null if supplied filter is empty`() {
    assertThat(buildProbationAreaFilter(listOf())).isNull()
  }

  @Test
  internal fun `will return a query with a "should" for each probation area supplied`() {
    assertThat(buildProbationAreaFilter(listOf("N01", "N02"))?.should()).hasSize(2)
  }

  @Test
  internal fun `will filter only for active offender managers`() {
    val nestedQuery = buildProbationAreaFilter(listOf("N01"))?.should()?.first() as NestedQueryBuilder
    val termQueries = nestedQuery.query() as BoolQueryBuilder
    assertThat(termQueries.must()).contains(QueryBuilders.termQuery("offenderManagers.active", true))
  }

  @Test
  internal fun `will filter offender managers probation area code`() {
    val nestedQuery = buildProbationAreaFilter(listOf("N01"))?.should()?.first() as NestedQueryBuilder
    val termQueries = nestedQuery.query() as BoolQueryBuilder
    assertThat(termQueries.must()).contains(QueryBuilders.termQuery("offenderManagers.probationArea.code", "N01"))
  }
}

fun getDefaultNamedXContents(): List<Entry> {
  // required by ES to parse the contents of our aggregations
  val contentParsers = mapOf(
    StringTerms.NAME to ContextParser<Any, Aggregation> { parser: XContentParser, name: Any ->
      ParsedStringTerms.fromXContent(
        parser,
        name as String
      )
    },
    NestedAggregationBuilder.NAME to ContextParser<Any, Aggregation> { parser: XContentParser, name: Any ->
      ParsedNested.fromXContent(
        parser,
        name as String
      )
    },
    LongTerms.NAME to ContextParser<Any, Aggregation> { parser: XContentParser, name: Any ->
      ParsedLongTerms.fromXContent(
        parser,
        name as String
      )
    },
    TopHitsAggregationBuilder.NAME to ContextParser<Any, Aggregation> { parser: XContentParser, name: Any ->
      ParsedTopHits.fromXContent(
        parser,
        name as String
      )
    },
  )
  return contentParsers
    .toList()
    .map { Entry(Aggregation::class.java, ParseField(it.first), it.second) }
}

fun String.toAggregation(): Aggregations {
  val parser = JSON.xContent().createParser(
    NamedXContentRegistry(getDefaultNamedXContents()),
    DeprecationHandler.THROW_UNSUPPORTED_OPERATION,
    this,
  )
  parser.nextToken() // simulate ES moving to first aggregation
  return Aggregations.fromXContent(parser)
}
