package uk.gov.justice.hmpps.probationsearch.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.hmpps.probationsearch.dto.IDs
import uk.gov.justice.hmpps.probationsearch.dto.OffenderDetail
import uk.gov.justice.hmpps.probationsearch.dto.OffenderUserAccess

internal class OffenderAccessServiceTest {
  private val communityService = mock<CommunityService>()
  private val offenderAccessService = OffenderAccessService(communityService)

  @BeforeEach
  internal fun setUp() {
    whenever(communityService.canAccessOffender(any())).thenReturn(AccessLimitation(userRestricted = false, userExcluded = false))
  }

  @Nested
  inner class DoesNotHaveEitherList {

    val offender = OffenderDetail(
      offenderId = 1,
      currentExclusion = false,
      currentRestriction = false,
      otherIds = IDs(crn = "X00001")
    )

    @Test
    fun `Won't make an access check regardless of ignores `() {
      offenderAccessService.canAccessOffender(
        offender,
        OffenderUserAccess(
          "sandra.black",
          ignoreExclusionsAlways = false,
          ignoreInclusionsAlways = false
        )
      )
      verify(communityService, never()).canAccessOffender(any())
    }

    @Test
    fun `Will return true `() {
      val canAccess = offenderAccessService.canAccessOffender(
        offender,
        OffenderUserAccess(
          "sandra.black",
          ignoreExclusionsAlways = false,
          ignoreInclusionsAlways = false
        )
      )
      assertThat(canAccess).isTrue
    }
  }

  @Nested
  inner class HasExclusionList {
    val offender = OffenderDetail(
      offenderId = 1,
      currentExclusion = true,
      currentRestriction = false,
      otherIds = IDs(crn = "X00001")
    )

    @Nested
    inner class WhenIgnoreExclusions {
      private val offenderUserAccess = OffenderUserAccess(
        "sandra.black",
        ignoreExclusionsAlways = true,
        ignoreInclusionsAlways = false
      )

      @Test
      fun `Won't make an access check`() {
        offenderAccessService.canAccessOffender(
          offender,
          offenderUserAccess
        )
        verify(communityService, never()).canAccessOffender(any())
      }

      @Test
      fun `Will return true `() {
        val canAccess = offenderAccessService.canAccessOffender(
          offender,
          offenderUserAccess
        )
        assertThat(canAccess).isTrue
      }
    }

    @Nested
    inner class WhenNotIgnoreExclusions {
      @Nested
      inner class WhenHasUsername {

        private val offenderUserAccess = OffenderUserAccess(
          "sandra.black",
          ignoreExclusionsAlways = false,
          ignoreInclusionsAlways = false
        )

        @Test
        fun `Will make an access check`() {
          offenderAccessService.canAccessOffender(
            offender,
            offenderUserAccess
          )
          verify(communityService).canAccessOffender("X00001")
        }

        @Test
        fun `Will return true if can userExcluded is false`() {
          whenever(communityService.canAccessOffender(any())).thenReturn(AccessLimitation(userRestricted = false, userExcluded = false))

          val canAccess = offenderAccessService.canAccessOffender(
            offender,
            offenderUserAccess
          )
          assertThat(canAccess).isTrue
        }

        @Test
        fun `Will return false if can userExcluded is true`() {
          whenever(communityService.canAccessOffender(any())).thenReturn(AccessLimitation(userRestricted = false, userExcluded = true))

          val canAccess = offenderAccessService.canAccessOffender(
            offender,
            offenderUserAccess
          )
          assertThat(canAccess).isFalse
        }
      }

      @Nested
      inner class WhenHasNoUsername {

        private val offenderUserAccess = OffenderUserAccess(
          null,
          ignoreExclusionsAlways = false,
          ignoreInclusionsAlways = false
        )

        @Test
        fun `Will not make an access check`() {
          offenderAccessService.canAccessOffender(
            offender,
            offenderUserAccess
          )
          verify(communityService, never()).canAccessOffender(any())
        }

        @Test
        fun `Will always return false`() {
          val canAccess = offenderAccessService.canAccessOffender(
            offender,
            offenderUserAccess
          )
          assertThat(canAccess).isFalse
        }
      }
    }
  }

  @Nested
  inner class HasInclusionList {
    val offender = OffenderDetail(
      offenderId = 1,
      currentExclusion = false,
      currentRestriction = true,
      otherIds = IDs(crn = "X00001")
    )

    @Nested
    inner class WhenIgnoreInclusions {
      private val offenderUserAccess = OffenderUserAccess(
        "sandra.black",
        ignoreExclusionsAlways = false,
        ignoreInclusionsAlways = true
      )

      @Test
      fun `Won't make an access check`() {
        offenderAccessService.canAccessOffender(
          offender,
          offenderUserAccess
        )
        verify(communityService, never()).canAccessOffender(any())
      }

      @Test
      fun `Will return true`() {
        val canAccess = offenderAccessService.canAccessOffender(
          offender,
          offenderUserAccess
        )
        assertThat(canAccess).isTrue
      }
    }

    @Nested
    inner class WhenNotIgnoreInclusions {

      @Nested
      inner class WhenHasNoUsername {
        private val offenderUserAccess = OffenderUserAccess(
          "sandra.black",
          ignoreExclusionsAlways = false,
          ignoreInclusionsAlways = false
        )

        @Test
        fun `Will make an access check`() {
          offenderAccessService.canAccessOffender(
            offender,
            offenderUserAccess
          )
          verify(communityService).canAccessOffender("X00001")
        }

        @Test
        fun `Will return true if can userRestricted is false`() {
          whenever(communityService.canAccessOffender(any())).thenReturn(AccessLimitation(userRestricted = false, userExcluded = false))

          val canAccess = offenderAccessService.canAccessOffender(
            offender,
            offenderUserAccess
          )
          assertThat(canAccess).isTrue
        }

        @Test
        fun `Will return false if can userRestricted is true`() {
          whenever(communityService.canAccessOffender(any())).thenReturn(AccessLimitation(userRestricted = true, userExcluded = false))

          val canAccess = offenderAccessService.canAccessOffender(
            offender,
            offenderUserAccess
          )
          assertThat(canAccess).isFalse
        }
      }
    }

    @Nested
    inner class WhenHasNoUsername {

      private val offenderUserAccess = OffenderUserAccess(
        null,
        ignoreExclusionsAlways = false,
        ignoreInclusionsAlways = false
      )

      @Test
      fun `Will not make an access check`() {
        offenderAccessService.canAccessOffender(
          offender,
          offenderUserAccess
        )
        verify(communityService, never()).canAccessOffender(any())
      }

      @Test
      fun `Will always return false`() {
        val canAccess = offenderAccessService.canAccessOffender(
          offender,
          offenderUserAccess
        )
        assertThat(canAccess).isFalse
      }
    }
  }
}
