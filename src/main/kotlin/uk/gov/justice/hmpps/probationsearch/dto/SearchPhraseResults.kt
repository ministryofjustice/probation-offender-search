package uk.gov.justice.hmpps.probationsearch.dto

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import io.swagger.v3.oas.annotations.media.Schema
import org.opensearch.search.suggest.Suggest
import org.springframework.boot.jackson.JsonComponent
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

class SearchPhraseResults(
  pageable: Pageable,
  content: Page<OffenderDetail>,
  total: Long,
  @Schema(description = "Counts of offenders aggregated by probation area") val probationAreaAggregations: List<ProbationAreaAggregation>,
  @Schema(description = "Alternative search phrase suggestions. See https://www.elastic.co/guide/en/elasticsearch/reference/current/search-suggesters.html") val suggestions: Suggest? = null,
) : Paged<OffenderDetail>(
  content = content.content,
  total = total,
  totalPages = content.totalPages.toLong(),
  totalElements = content.totalElements,
  size = pageable.pageSize.toLong(),
  number = pageable.pageNumber.toLong(),
  numberOfElements = content.numberOfElements.toLong(),
  first = content.isFirst,
  last = content.isLast,
  empty = content.isEmpty,
  sort = content.sort,
  pageable = pageable,
)

open class Paged<T>(
  val content: List<T>,
  val total: Long,
  val totalPages: Long,
  val totalElements: Long,
  val last: Boolean,
  val size: Long,
  val number: Long,
  val numberOfElements: Long,
  val first: Boolean,
  val empty: Boolean,
  val pageable: Pageable,
  val sort: Sort,
  val page: PageObject = PageObject(
    size = pageable.pageSize.toLong(),
    number = pageable.pageNumber.toLong(),
    totalElements = totalElements,
    totalPages = totalPages,
  ),
)

data class PageObject(
  val size: Long,
  val number: Long,
  val totalElements: Long,
  val totalPages: Long,
)

data class ProbationAreaAggregation(
  @Schema(description = "Probation area code", example = "N07") val code: String,
  @Schema(description = "Probation area description", example = "London") val description: String?,
  @Schema(description = "Count of matching offenders in this area", example = "78") val count: Long,
)

@JsonComponent
class SuggestSerializer : JsonSerializer<Suggest>() {
  override fun serialize(value: Suggest, gen: JsonGenerator, serializers: SerializerProvider?) {
    gen.writeRawValue(value.toString())
  }
}
