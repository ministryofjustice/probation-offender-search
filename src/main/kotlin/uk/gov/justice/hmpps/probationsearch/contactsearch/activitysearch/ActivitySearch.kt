package uk.gov.justice.hmpps.probationsearch.contactsearch.activitysearch

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.elasticsearch.annotations.DateFormat
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime

data class ActivitySearchRequest(
  val crn: String,
  val keywords: String? = "",
  val dateFrom: LocalDate? = null,
  val dateTo: LocalDate? = null,
  @Schema(
    description = "Whether to include system generated contacts in the search results. Defaults to true.",
    example = "false"
  ) val includeSystemGenerated: Boolean = true,
  val filters: List<String> = emptyList(),
)

data class ActivitySearchAuditRequest(
  val search: ActivitySearchRequest,
  val username: String,
  val pagination: PageRequest,
  val dateTime: ZonedDateTime = ZonedDateTime.now(),
) {
  data class PageRequest(
    val page: Int,
    val pageSize: Int,
    val sort: String?,
    val direction: String?,
  )
}

data class ActivitySearchResponse(
  val size: Int,
  val page: Int,
  val totalResults: Long,
  val totalPages: Int,
  val results: List<ActivitySearchResult>,
)

data class ActivitySearchResult(
  val crn: String,
  val id: Long,
  val typeCode: String,
  val typeDescription: String,
  val outcomeCode: String?,
  val outcomeDescription: String?,
  val description: String?,
  val notes: String?,
  @field:Field(type = FieldType.Date, format = [DateFormat.date])
  val date: LocalDate,
  @field:Field(type = FieldType.Date, format = [DateFormat.hour_minute_second])
  val startTime: LocalTime?,
  @field:Field(type = FieldType.Date, format = [DateFormat.hour_minute_second])
  val endTime: LocalTime?,
  @field:Field(type = FieldType.Date, format = [DateFormat.date_hour_minute_second])
  val lastUpdatedDateTime: LocalDateTime,
  val highlights: Map<String, List<String>> = mapOf(),
  @field:JsonInclude(JsonInclude.Include.NON_NULL)
  val score: Double?,
  val complied: String? = null,
  @field:Field(type = FieldType.Date, format = [DateFormat.date_hour_minute_second])
  val startDateTime: LocalDateTime?,
  @field:Field(type = FieldType.Date, format = [DateFormat.date_hour_minute_second])
  val endDateTime: LocalDateTime?,
  val requiresOutcome: String?,
  val outcomeRequiredFlag: String?,
  val nationalStandard: String?,
  val systemGenerated: String?,
)