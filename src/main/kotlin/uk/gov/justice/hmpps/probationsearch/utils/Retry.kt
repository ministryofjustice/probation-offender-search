package uk.gov.justice.hmpps.probationsearch.utils

import java.util.concurrent.TimeUnit.MILLISECONDS

object Retry {
  fun <T> retry(
    attempts: Int = 3,
    delays: List<Long> = listOf(0, 250, 1000, 5000),
    operation: () -> T,
  ): T {
    repeat(attempts) { attempt ->
      try {
        return operation()
      } catch (e: Exception) {
        if (attempt >= attempts - 1) throw e
        MILLISECONDS.sleep(delays.getOrElse(attempt - 1) { delays.last() })
      }
    }
    error("Retry error")
  }
}
