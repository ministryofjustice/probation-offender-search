package uk.gov.justice.hmpps.probationsearch.dto

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import io.swagger.v3.oas.annotations.media.Schema
import org.opensearch.search.suggest.Suggest
import org.springframework.boot.jackson.JsonComponent
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

class SearchPhraseResults(
  content: List<OffenderDetail>,
  pageable: Pageable,
  total: Long,
  @Schema(description = "Counts of offenders aggregated by probation area") val probationAreaAggregations: List<ProbationAreaAggregation>,
  @Schema(description = "Alternative search phrase suggestions. See https://www.elastic.co/guide/en/elasticsearch/reference/current/search-suggesters.html") val suggestions: Suggest? = null,
) : PageImpl<OffenderDetail>(content, pageable, total)

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
