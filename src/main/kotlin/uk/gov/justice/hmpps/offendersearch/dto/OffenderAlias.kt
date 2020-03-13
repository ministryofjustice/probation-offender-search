package uk.gov.justice.hmpps.offendersearch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OffenderAlias {
    private LocalDate dateOfBirth;
    private String firstName;
    private List<String> middleNames;
    private String surname;
    private String gender;

}
