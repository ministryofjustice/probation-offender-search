package uk.gov.justice.hmpps.offendersearch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneNumber {
    private PhoneTypes type;
    private Optional<String> number;

    public enum PhoneTypes {TELEPHONE, MOBILE}
}
