package uk.gov.justice.hmpps.probationsearch.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class CanonicalPncKtTest {

  @Test
  internal fun `will convert to canonical form of PNC when it is valid`() {
    assertThat("2003/1234567A".canonicalPNCNumber()).isEqualTo("2003/1234567a")
    assertThat("2003/0234567A".canonicalPNCNumber()).isEqualTo("2003/234567a")
    assertThat("2003/0034567A".canonicalPNCNumber()).isEqualTo("2003/34567a")
    assertThat("2003/0004567A".canonicalPNCNumber()).isEqualTo("2003/4567a")
    assertThat("2003/0000567A".canonicalPNCNumber()).isEqualTo("2003/567a")
    assertThat("2003/0000067A".canonicalPNCNumber()).isEqualTo("2003/67a")
    assertThat("2003/0000007A".canonicalPNCNumber()).isEqualTo("2003/7a")
    assertThat("2003/0000000A".canonicalPNCNumber()).isEqualTo("2003/0a")
    assertThat("03/1234567A".canonicalPNCNumber()).isEqualTo("03/1234567a")
    assertThat("03/0234567A".canonicalPNCNumber()).isEqualTo("03/234567a")
    assertThat("03/0034567A".canonicalPNCNumber()).isEqualTo("03/34567a")
    assertThat("03/0004567A".canonicalPNCNumber()).isEqualTo("03/4567a")
    assertThat("03/0000567A".canonicalPNCNumber()).isEqualTo("03/567a")
    assertThat("03/0000067A".canonicalPNCNumber()).isEqualTo("03/67a")
    assertThat("03/0000007A".canonicalPNCNumber()).isEqualTo("03/7a")
    assertThat("03/0000000A".canonicalPNCNumber()).isEqualTo("03/0a")
  }

  @Test
  internal fun `will not convert to canonical form of PNC when it is not valid`() {
    assertThat("2003/A".canonicalPNCNumber()).isEqualTo("2003/A")
    assertThat("203/1234567A".canonicalPNCNumber()).isEqualTo("203/1234567A")
    assertThat("203/1234567".canonicalPNCNumber()).isEqualTo("203/1234567")
    assertThat("1234567A".canonicalPNCNumber()).isEqualTo("1234567A")
    assertThat("2013".canonicalPNCNumber()).isEqualTo("2013")
    assertThat("john smith".canonicalPNCNumber()).isEqualTo("john smith")
    assertThat("john/smith".canonicalPNCNumber()).isEqualTo("john/smith")
    assertThat("16/11/2018".canonicalPNCNumber()).isEqualTo("16/11/2018")
    assertThat("16-11-2018".canonicalPNCNumber()).isEqualTo("16-11-2018")
    assertThat("111111/11A".canonicalPNCNumber()).isEqualTo("111111/11A")
    assertThat("SF68/945674U".canonicalPNCNumber()).isEqualTo("SF68/945674U")
    assertThat("".canonicalPNCNumber()).isEqualTo("")
    assertThat("2010/BBBBBBBA".canonicalPNCNumber()).isEqualTo("2010/BBBBBBBA")
  }
}
