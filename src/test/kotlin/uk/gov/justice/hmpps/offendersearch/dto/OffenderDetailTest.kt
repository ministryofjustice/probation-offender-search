package uk.gov.justice.hmpps.offendersearch.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class OffenderDetailTest {

  @Test
  fun `age is calculated from date of birth`() {
    val expectedAge = LocalDate.now().year - 2000
    assertThat(OffenderDetail(offenderId = 99, dateOfBirth = LocalDate.parse("2000-01-01")).age).isEqualTo(expectedAge)
  }
  @Test
  fun `age is not present if date of birth is not present`() {
    assertThat(OffenderDetail(offenderId = 99, dateOfBirth = null).age).isNull()
  }
}
