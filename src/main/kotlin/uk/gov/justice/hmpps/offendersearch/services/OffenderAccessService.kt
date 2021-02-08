package uk.gov.justice.hmpps.offendersearch.services

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.offendersearch.dto.OffenderDetail
import uk.gov.justice.hmpps.offendersearch.dto.OffenderUserAccess

@Service
class OffenderAccessService(private val communityService: CommunityService) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun canAccessOffender(offenderDetail: OffenderDetail, offenderUserAccess: OffenderUserAccess): Boolean =
    if (shouldCheckAccess(offenderDetail, offenderUserAccess)) {
      offenderUserAccess.username?.let {
        canAccess(communityService.canAccessOffender(offenderDetail.otherIds!!.crn!!))
          .also { log.debug("Access to ${offenderDetail.otherIds.crn!!} for ${offenderUserAccess.username} was allowed=$it") }
      }
        ?: false.also { log.debug("No user in context when checking access to ${offenderDetail.otherIds!!.crn!!}, so access denied") }
    } else {
      true
    }
}

private fun shouldCheckAccess(offenderDetail: OffenderDetail, offenderUserAccess: OffenderUserAccess): Boolean =
  (offenderDetail.currentExclusion ?: false && offenderUserAccess.ignoreExclusionsAlways.not()) ||
    (offenderDetail.currentRestriction ?: false && offenderUserAccess.ignoreInclusionsAlways.not())

private fun canAccess(accessLimitation: AccessLimitation): Boolean =
  accessLimitation.userExcluded.not() && accessLimitation.userRestricted.not()
