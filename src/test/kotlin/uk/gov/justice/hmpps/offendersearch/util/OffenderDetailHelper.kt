package uk.gov.justice.hmpps.offendersearch.util

import uk.gov.justice.hmpps.offendersearch.dto.*
import java.time.LocalDate

interface OffenderDetailHelper {
  companion object {
    fun anOffender(): OffenderDetail? {
      return OffenderDetail(
          otherIds = IDs(crn = "crn123", croNumber = "cro1234", niNumber = "nx12345A", nomsNumber = "AN00AB", immigrationNumber = "QS122", mostRecentPrisonerNumber = "12345", pncNumber = "pnc123"),
            dateOfBirth = LocalDate.of(1970, 1, 1),
            exclusionMessage  = "exclusion message",
            firstName  = "Hannah",
            gender  = "female",
            previousSurname  = "Jones",
            restrictionMessage  = "Restriction message",
            surname  = "Mann",
            title  = "Miss",          
          offenderAliases = listOf(OffenderAlias()),
          partitionArea = "Hallam",
          softDeleted = false,
          currentDisposal = "cd",
          currentRestriction = false,
          currentExclusion = true,          
          offenderManagers = listOf(OffenderManager(
              probationArea = ProbationArea(code = "A", description = "B"))),
          offenderProfile = OffenderProfile(
              offenderLanguages = OffenderLanguages(primaryLanguage = "English", otherLanguages = listOf("French"), requiresInterpreter = false),
              ethnicity = "ethnicity",
              disabilities = listOf(Disability())),
          contactDetails = ContactDetails(
              addresses = listOf(Address(addressNumber = "123"))))
    }

    fun anOffenderList(): List<OffenderDetail?>? {
      return listOf(anOffender())
    }
  }
}