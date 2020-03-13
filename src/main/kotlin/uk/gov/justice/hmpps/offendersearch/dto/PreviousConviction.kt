package uk.gov.justice.hmpps.offendersearch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PreviousConviction {
    private LocalDate convictionDate;
    private Map<String, String> detail;
}
