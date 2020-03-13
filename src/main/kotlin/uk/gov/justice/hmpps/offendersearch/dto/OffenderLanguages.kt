package uk.gov.justice.hmpps.offendersearch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class OffenderLanguages {
    private String primaryLanguage;
    private List<String> otherLanguages;
    private String languageConcerns;
    private Boolean requiresInterpreter;
}
