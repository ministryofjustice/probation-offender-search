import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.hmpps.probationsearch.utils.Retry.retry

class RetryTest {
  @Test
  fun `should succeed on the first attempt with default settings`() {
    var attempts = 0
    val result = retry {
      attempts++
      "Success"
    }

    assertEquals("Success", result)
    assertEquals(1, attempts)
  }

  @Test
  fun `should succeed after two retries (3 total attempts) with default settings`() {
    var attempts = 0
    val result = retry {
      attempts++
      if (attempts <= 2) error("Failing attempt $attempts")
      "Success on $attempts"
    }

    assertEquals("Success on 3", result)
    assertEquals(3, attempts)
  }

  @Test
  fun `should fail and re-throw exception after all default attempts`() {
    var attempts = 0
    val finalException = assertThrows<IllegalStateException> {
      retry {
        attempts++
        error("Failing attempt $attempts")
      }
    }

    assertEquals(3, attempts)
    assertEquals("Failing attempt 3", finalException.message)
  }
}
