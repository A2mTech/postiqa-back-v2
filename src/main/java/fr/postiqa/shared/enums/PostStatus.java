package fr.postiqa.shared.enums;

/**
 * Status of a post throughout its lifecycle.
 */
public enum PostStatus {
    DRAFT("Draft"),
    SCHEDULED("Scheduled"),
    PUBLISHING("Publishing"),
    PUBLISHED("Published"),
    FAILED("Failed"),
    CANCELLED("Cancelled");

    private final String displayName;

    PostStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Check if the post can be edited
     */
    public boolean isEditable() {
        return this == DRAFT || this == SCHEDULED;
    }

    /**
     * Check if the post can be scheduled
     */
    public boolean canBeScheduled() {
        return this == DRAFT;
    }

    /**
     * Check if the post can be cancelled
     */
    public boolean canBeCancelled() {
        return this == SCHEDULED;
    }

    /**
     * Check if the post is in a final state
     */
    public boolean isFinal() {
        return this == PUBLISHED || this == FAILED || this == CANCELLED;
    }
}
