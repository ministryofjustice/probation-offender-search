package uk.gov.justice.hmpps.offendersearch.services

import com.github.tomakehurst.wiremock.client.WireMock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.hmpps.offendersearch.dto.IDs
import uk.gov.justice.hmpps.offendersearch.dto.MatchRequest
import uk.gov.justice.hmpps.offendersearch.dto.OffenderDetail
import uk.gov.justice.hmpps.offendersearch.dto.OffenderMatch
import uk.gov.justice.hmpps.offendersearch.wiremock.HmppsPersonMatchScoreExtension
import java.time.LocalDate

@ExtendWith(HmppsPersonMatchScoreExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["test"])
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
internal class MatchScoreServiceTest {
  @Autowired
  private lateinit var matchScoreService : MatchScoreService

  @Nested
  inner class Score {
    @Test
    fun `return a score for a match`() {
      val matchRequest = MatchRequest(
        firstName = "ann",
        surname = "gramsci",
        dateOfBirth = LocalDate.of(1988, 1, 6),
        pncNumber = "2018/0123456X"
      )

      val match = OffenderMatch(
        OffenderDetail(
          firstName = "anne",
          surname = "gramsci",
          dateOfBirth = LocalDate.of(1988, 1, 6),
          offenderId = 123,
          otherIds = IDs(pncNumber = "2018/0123456X")
        )
      )

      HmppsPersonMatchScoreExtension.hmppsPersonMatchScore.stubPersonMatchScore()

      val matchScore = matchScoreService.score(matchRequest, match).block()

      HmppsPersonMatchScoreExtension.hmppsPersonMatchScore.verify(
        WireMock.postRequestedFor(WireMock.urlEqualTo("/match"))
      )

      assertThat(matchScore?.matchProbability).isEqualTo(0.9172587927)
    }

    @Test
    fun `if the match score request fails then return empty`() {
      val matchRequest = MatchRequest(
        firstName = "ann",
        surname = "gramsci",
        dateOfBirth = LocalDate.of(1988, 1, 6),
        pncNumber = "2018/0123456X"
      )

      val match = OffenderMatch(
        OffenderDetail(
          firstName = "anne",
          surname = "gramsci",
          dateOfBirth = LocalDate.of(1988, 1, 6),
          offenderId = 123,
          otherIds = IDs(pncNumber = "2018/0123456X")
        )
      )

      HmppsPersonMatchScoreExtension.hmppsPersonMatchScore.stubPersonMatchScoreError()

      val matchScore = matchScoreService.score(matchRequest, match).block()

      HmppsPersonMatchScoreExtension.hmppsPersonMatchScore.verify(
        WireMock.postRequestedFor(WireMock.urlEqualTo("/match"))
      )

      assertThat(matchScore?.matchProbability).isNull()
    }
  }
}
