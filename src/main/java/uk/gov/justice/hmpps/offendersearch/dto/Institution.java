package uk.gov.justice.hmpps.offendersearch.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Institution {
    @ApiModelProperty(required = true)
    private Long institutionId;
    private Boolean isEstablishment;
    private String code;
    private String description;
    private String institutionName;
    private KeyValue establishmentType;
    private Boolean isPrivate;
    @ApiModelProperty(value = "Prison institution code in NOMIS")
    private String nomsPrisonInstitutionCode;
}
