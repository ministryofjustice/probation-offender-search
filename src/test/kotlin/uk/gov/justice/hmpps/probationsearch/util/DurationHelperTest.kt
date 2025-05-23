package uk.gov.justice.hmpps.probationsearch.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.hmpps.probationsearch.utils.DurationHelper
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal class DurationHelperTest {
  companion object {
    @JvmStatic
    fun datesAndDurations() = listOf(
      arguments("Not a date", Duration.ofMinutes(5), false),
      arguments("2022-11-19T08:22:13.207Z", Duration.ofMinutes(5), true),
      arguments(DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now().minusMinutes(4)), Duration.ofMinutes(5), false ),
      arguments(DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now().minusMinutes(6)), Duration.ofMinutes(5), true )
    )
  }

  @ParameterizedTest
  @MethodSource("datesAndDurations")
  internal fun `will determine if a string is a date longer ago than the duration`(date: String, duration: Duration, expected: Boolean) {
    assertThat(DurationHelper.longerThanAgo(date, duration)).isEqualTo(expected)
  }
}
