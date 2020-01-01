package uk.gov.justice.hmpps.offendersearch.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchDto {
    @ApiModelProperty(required = false, value = "Offender first name(s)", position = 1)
    private String firstName;
    @ApiModelProperty(required = false, value = "Offender surname", position = 2)
    private String surname;
    @ApiModelProperty(required = false, value = "Offender date of birth", example = "1996-02-10", position = 3)
    private LocalDate dateOfBirth;
    @ApiModelProperty(required = false, value = "Police National Computer (PNC) number", example = "", position = 4)
    private String pncNumber;
    @ApiModelProperty(required = false, value = "Criminal Records Office (CRO) number", example = "", position = 5)
    private String croNumber;
    @ApiModelProperty(required = false, value = "", example = "", position = 6)
    private String crn;
    @ApiModelProperty(required = false, value = "The Offender NOMIS Id (aka prison number/offender no in DPS)", example = "G5555TT", position = 7)
    private String nomsNumber;

    public boolean isValid(){
        return (firstName!=null || surname!=null || dateOfBirth!=null || pncNumber!=null || crn!=null || nomsNumber!=null || croNumber!=null);
    }
}
