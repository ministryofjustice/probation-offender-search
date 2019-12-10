package uk.gov.justice.hmpps.offendersearch;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String msg) {
        super(msg);
    }
}
