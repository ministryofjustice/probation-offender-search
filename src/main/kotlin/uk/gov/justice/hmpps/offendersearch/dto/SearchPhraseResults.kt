package uk.gov.justice.hmpps.offendersearch.dto

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import io.swagger.annotations.ApiModelProperty
import org.elasticsearch.search.suggest.Suggest
import org.springframework.boot.jackson.JsonComponent

data class SearchPhraseResults(
    @ApiModelProperty(value = "List of matching offenders in this page") val offenders: List<OffenderDetail>,
    @ApiModelProperty(value = "Total number of matching offenders") val total: Long,
    @ApiModelProperty(value = "Counts of offenders aggregated by probation area") val probationAreaAggregations: List<ProbationAreaAggregation>,
    @ApiModelProperty(value = "Alternative search phrase suggestions. See https://www.elastic.co/guide/en/elasticsearch/reference/current/search-suggesters.html") val suggestions: Suggest? = null
)

data class ProbationAreaAggregation(
    @ApiModelProperty(value = "Probation area code", example = "N02") val code: String,
    @ApiModelProperty(value = "Probation area description", example = "NPS North East") val description: String,
    @ApiModelProperty(value = "Count of matching offenders in this area", example = "78") val count: Long
)


@JsonComponent
class SuggestSerializer : JsonSerializer<Suggest>() {
  override fun serialize(value: Suggest, gen: JsonGenerator, serializers: SerializerProvider?) {
    // ES provides a convent toString() on Suggest that converts to JSON, we just need to add the missing colon
    gen.writeRaw(":$value")
  }

}