package fr.postiqa.features.postmanagement.domain.exception;

/**
 * Exception thrown when media upload fails.
 */
public class MediaUploadException extends DomainException {

    private final String fileName;
    private final String reason;

    public MediaUploadException(String fileName, String reason) {
        super(String.format("Media upload failed for %s: %s", fileName, reason));
        this.fileName = fileName;
        this.reason = reason;
    }

    public MediaUploadException(String fileName, String reason, Throwable cause) {
        super(String.format("Media upload failed for %s: %s", fileName, reason), cause);
        this.fileName = fileName;
        this.reason = reason;
    }

    public String getFileName() {
        return fileName;
    }

    public String getReason() {
        return reason;
    }
}
