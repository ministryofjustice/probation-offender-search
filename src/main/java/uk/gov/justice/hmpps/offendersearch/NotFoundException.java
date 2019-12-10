package uk.gov.justice.hmpps.offendersearch;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String msg) {
        super(msg);
    }
}