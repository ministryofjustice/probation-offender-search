package uk.gov.justice.hmpps.probationsearch.contactsearch.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.data.elasticsearch.annotations.DateFormat
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class ContactSearchResponse(
  val size: Int,
  val page: Int,
  val totalResults: Long,
  val totalPages: Int,
  val results: List<ContactSearchResult>,
)

data class ContactSearchResult(
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
  @field:JsonIgnore
  val semanticMatch: Boolean = false,
)
