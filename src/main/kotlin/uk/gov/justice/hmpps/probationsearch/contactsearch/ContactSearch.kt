package uk.gov.justice.hmpps.probationsearch.contactsearch

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.Size
import org.springframework.data.elasticsearch.annotations.DateFormat
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime

class ContactSearchRequest(
  val crn: String,
  query: String? = "",
  val matchAllTerms: Boolean = true,
  includeScores: Boolean? = false,
) {
  @Size(max = 1000, message = "query length must not exceed 1000 characters")
  val query = query ?: ""
  val includeScores: Boolean = includeScores ?: false
}

data class ContactSearchAuditRequest(
  val search: ContactSearchRequest,
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
)
