package uk.gov.justice.hmpps.offendersearch.dto

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import io.swagger.annotations.ApiModelProperty
import org.elasticsearch.search.suggest.Suggest
import org.springframework.boot.jackson.JsonComponent
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

class SearchPhraseResults(
    content: List<OffenderDetail>,
    pageable: Pageable,
    total: Long,
    @ApiModelProperty(value = "Counts of offenders aggregated by probation area") val probationAreaAggregations: List<ProbationAreaAggregation>,
    @ApiModelProperty(value = "Alternative search phrase suggestions. See https://www.elastic.co/guide/en/elasticsearch/reference/current/search-suggesters.html") val suggestions: Suggest? = null
) : PageImpl<OffenderDetail>(content, pageable, total)

data class ProbationAreaAggregation(
    @ApiModelProperty(value = "Probation area code", example = "N02") val code: String,
    @ApiModelProperty(value = "Count of matching offenders in this area", example = "78") val count: Long
)


@JsonComponent
class SuggestSerializer : JsonSerializer<Suggest>() {
  override fun serialize(value: Suggest, gen: JsonGenerator, serializers: SerializerProvider?) {
    gen.writeRawValue(value.toString())
  }
}