package uk.gov.justice.hmpps.offendersearch.dto

import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

class SearchPageResults(
  content: List<OffenderDetail>,
  pageable: Pageable,
  total: Long,
) : PageImpl<OffenderDetail>(content, pageable, total)

// data class ProbationAreaAggregation(
//   @ApiModelProperty(value = "Probation area code", example = "N02") val code: String,
//   @ApiModelProperty(value = "Count of matching offenders in this area", example = "78") val count: Long
// )
//
// @JsonComponent
// class SuggestSerializer : JsonSerializer<Suggest>() {
//   override fun serialize(gen: JsonGenerator, serializers: SerializerProvider?) {
//     gen.writeRawValue(value.toString())
//   }
// }
