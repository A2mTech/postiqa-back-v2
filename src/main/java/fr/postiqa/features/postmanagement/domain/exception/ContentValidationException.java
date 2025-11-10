package fr.postiqa.features.postmanagement.domain.exception;

/**
 * Exception thrown when content validation fails.
 */
public class ContentValidationException extends DomainException {

    private final String field;
    private final String reason;

    public ContentValidationException(String field, String reason) {
        super(String.format("Content validation failed for %s: %s", field, reason));
        this.field = field;
        this.reason = reason;
    }

    public ContentValidationException(String message) {
        super(message);
        this.field = null;
        this.reason = message;
    }

    public String getField() {
        return field;
    }

    public String getReason() {
        return reason;
    }
}
