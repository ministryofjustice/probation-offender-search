package uk.gov.justice.hmpps.offendersearch.util;

import org.assertj.core.util.Lists;
import uk.gov.justice.hmpps.offendersearch.dto.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public interface OffenderDetailHelper {
    static OffenderDetail anOffender() {
        return OffenderDetail.builder()
                .otherIds(IDs.builder().crn("crn123").croNumber("cro1234").niNumber("nx12345A").nomsNumber("AN00AB").immigrationNumber("QS122").mostRecentPrisonerNumber("12345").pncNumber("pnc123").build())
                .dateOfBirth(LocalDate.of(1970, 1, 1))
                .exclusionMessage("exclusion message")
                .firstName("Hannah")
                .gender("female")
                .previousSurname("Jones")
                .restrictionMessage("Restriction message")
                .surname("Mann")
                .title("Miss")
                .offenderAliases(Lists.newArrayList(OffenderAlias.builder().build()))
                .partitionArea("Hallam")
                .softDeleted(false)
                .currentDisposal("cd")
                .currentRestriction(false)
                .currentExclusion(true)
                .offenderManagers(Lists.newArrayList(OffenderManager.builder()
                        .probationArea(ProbationArea.builder().code("A").description("B").build())
                        .build()))
                .offenderProfile(OffenderProfile.builder()
                        .offenderLanguages(OffenderLanguages.builder().primaryLanguage("English").otherLanguages(Collections.singletonList("French")).requiresInterpreter(false).build())
                        .ethnicity("ethnicity")
                        .disabilities(Collections.singletonList(Disability.builder().build())).build())
                .contactDetails(ContactDetails.builder()
                        .addresses(Collections.singletonList(Address.builder().addressNumber("123").build())).build())
                .build();
    }

    static List<OffenderDetail> anOffenderList() {
        return Collections.singletonList(anOffender());
    }
}
