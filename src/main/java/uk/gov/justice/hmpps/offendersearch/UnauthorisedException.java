package uk.gov.justice.hmpps.offendersearch;

public class UnauthorisedException extends RuntimeException {
    public UnauthorisedException(String msg) {
        super(msg);
    }
}