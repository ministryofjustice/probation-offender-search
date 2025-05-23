package uk.gov.justice.hmpps.probationsearch.utils

import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object DurationHelper {
  fun longerThanAgo(dateInPast: String, durationInPast: Duration): Boolean {
    val creationDate = try {
      LocalDateTime.parse(dateInPast, DateTimeFormatter.ISO_DATE_TIME)
    } catch (_: Exception) {
      null
    }
    if (creationDate != null) {
      val duration = Duration.between(creationDate, LocalDateTime.now())
      return duration.seconds > durationInPast.seconds
    }
    return false
  }
}