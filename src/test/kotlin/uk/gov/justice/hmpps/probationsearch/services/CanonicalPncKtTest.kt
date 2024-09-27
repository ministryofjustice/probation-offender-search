package uk.gov.justice.hmpps.probationsearch.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource

internal class CanonicalPncKtTest {
  companion object {
    @JvmStatic
    fun validPNCs() = listOf(
      arguments("2003/1234567A", "2003/1234567a"),
      arguments("2003/0234567A", "2003/234567a"),
      arguments("2003/0034567A", "2003/34567a"),
      arguments("2003/0004567A", "2003/4567a"),
      arguments("2003/0000567A", "2003/567a"),
      arguments("2003/0000067A", "2003/67a"),
      arguments("2003/0000007A", "2003/7a"),
      arguments("2003/0000000A", "2003/0a"),
      arguments("03/1234567A", "03/1234567a"),
      arguments("03/0234567A", "03/234567a"),
      arguments("03/0034567A", "03/34567a"),
      arguments("03/0004567A", "03/4567a"),
      arguments("03/0000567A", "03/567a"),
      arguments("03/0000067A", "03/67a"),
      arguments("03/0000007A", "03/7a"),
      arguments("03/0000000A", "03/0a"),
    )

    @JvmStatic
    fun invalidPNCs() = listOf(
      arguments("203/1234567A"),
      arguments("203/1234567"),
      arguments("1234567A"),
      arguments("2013"),
      arguments("john smith"),
      arguments("john/smith"),
      arguments("16/11/2018"),
      arguments("16-11-2018"),
      arguments("111111/11A"),
      arguments("SF68/945674U"),
      arguments(""),
      arguments("2010/BBBBBBBA"),
      arguments("2003/012345678A"),
    )
  }

  @ParameterizedTest
  @MethodSource("validPNCs")
  internal fun `will convert to canonical form of PNC when it is valid`(actual: String, expected: String) {
    assertThat(actual.canonicalPNCNumber()).isEqualTo(expected)
  }

  @ParameterizedTest
  @MethodSource("invalidPNCs")
  internal fun `will not convert to canonical form of PNC when it is not valid`(actual: String) {
    assertThat(actual.canonicalPNCNumber()).isEqualTo(actual)
  }
}
