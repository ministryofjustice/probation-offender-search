package uk.gov.justice.hmpps.offendersearch.services

import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilder
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


fun BoolQueryBuilder.mustMatchOneOf(query: String, values: List<Any>): BoolQueryBuilder {
  val nestedQuery = QueryBuilders.boolQuery()
  values.forEach { nestedQuery.should(QueryBuilders.boolQuery().must(query, it)) }
  return this.must(nestedQuery)
}

fun BoolQueryBuilder.mustIfPresent(maybeSimpleTermQuery: QueryBuilder?): BoolQueryBuilder {
  maybeSimpleTermQuery
      ?.let {
        this.must().add(it)
      }
  return this
}

fun BoolQueryBuilder.mustAll(queries: List<QueryBuilder>): BoolQueryBuilder {
  queries.forEach {
    this.must().add(it)
  }
  return this
}

fun BoolQueryBuilder.shouldIfPresent(maybeSimpleTermQuery: QueryBuilder?): BoolQueryBuilder {
  maybeSimpleTermQuery
      ?.let {
        this.should().add(it)
      }
  return this
}

fun BoolQueryBuilder.shouldAll(queries: List<QueryBuilder>): BoolQueryBuilder {
  queries.forEach {
    this.should().add(it)
  }
  return this
}

