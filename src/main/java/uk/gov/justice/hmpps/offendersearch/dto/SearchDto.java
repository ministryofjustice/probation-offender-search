package uk.gov.justice.hmpps.offendersearch.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchDto {
    //todo confirm description and examples
    @ApiModelProperty(required = false, value = "Offender first name(s)", position = 1)
    private String firstName;
    @ApiModelProperty(required = false, value = "Offender surname", position = 2)
    private String surname;
    @ApiModelProperty(required = false, value = "Offender date of birth", example = "1996-02-10", position = 3)
    private LocalDate dateOfBirth;
    @ApiModelProperty(required = false, value = "Police National Computer (PNC) number", example = "2018/0123456X", position = 4)
    private String pncNumber;
    @ApiModelProperty(required = false, value = "Criminal Records Office (CRO) number", example = "SF80/655108T", position = 5)
    private String croNumber;
    @ApiModelProperty(required = false, value = "Crime reference number", example = "X00001", position = 6)
    private String crn;
    @ApiModelProperty(required = false, value = "The Offender NOMIS Id (aka prison number/offender no in DPS)", example = "G5555TT", position = 7)
    private String nomsNumber;

    public boolean isValid(){
        return (StringUtils.isNotBlank(firstName) || StringUtils.isNotBlank(surname) || dateOfBirth!=null || StringUtils.isNotBlank(pncNumber) || StringUtils.isNotBlank(crn) ||
                StringUtils.isNotBlank(nomsNumber) || StringUtils.isNotBlank(croNumber));
    }
}
