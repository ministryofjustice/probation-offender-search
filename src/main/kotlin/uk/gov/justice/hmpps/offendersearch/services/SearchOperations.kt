package uk.gov.justice.hmpps.offendersearch.services

import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilders

fun BoolQueryBuilder.mustWhenPresent(query: String, value: Any?): BoolQueryBuilder {
  value.takeIf {
    when(it) {
      is String -> it.isNotBlank()
      else -> true
    }
  }?.let {
    this.must(QueryBuilders.matchQuery(query, it))
  }
  return this
}

fun BoolQueryBuilder.shouldMultiMatch(value: Any?, vararg query: String): BoolQueryBuilder {
  value.takeIf {
    when(it) {
      is String -> it.isNotBlank()
      else -> true
    }
  }?.let {
    this.should().add(QueryBuilders.multiMatchQuery(value, *query))
  }
  return this
}

fun BoolQueryBuilder.must(query: String, value: Any): BoolQueryBuilder {
  this.must(QueryBuilders.matchQuery(query, value))
  return this
}

fun BoolQueryBuilder.mustWhenTrue(predicate: () -> Boolean, query: String, value: String): BoolQueryBuilder {
  value.takeIf { predicate() }?.let {
    this.must(QueryBuilders.matchQuery(query, it))
  }
  return this
}

fun BoolQueryBuilder.mustMultiMatchKeyword(value: Any?, vararg query: String): BoolQueryBuilder {
  value.takeIf {
    when(it) {
      is String -> it.isNotBlank()
      else -> true
    }
  }?.let {
    this.must().add(QueryBuilders.multiMatchQuery(value, *query)
        .analyzer("keyword")
    )
  }
  return this
}

fun BoolQueryBuilder.mustMultiMatch(value: Any?, vararg query: String): BoolQueryBuilder {
  value.takeIf {
    when(it) {
      is String -> it.isNotBlank()
      else -> true
    }
  }?.let {
    this.must().add(QueryBuilders.multiMatchQuery(value, *query)
    )
  }
  return this
}

fun BoolQueryBuilder.mustKeyword(value: Any?, query: String): BoolQueryBuilder {
  value.takeIf {
    when(it) {
      is String -> it.isNotBlank()
      else -> true
    }
  }?.let {
    return this.must(QueryBuilders.matchQuery(query, value).analyzer("keyword"))
  }
  return this
}