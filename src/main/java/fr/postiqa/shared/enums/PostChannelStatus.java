package fr.postiqa.shared.enums;

/**
 * Status of a post for a specific channel/platform.
 * Used to track multi-platform publishing status.
 */
public enum PostChannelStatus {
    PENDING("Pending"),
    PUBLISHING("Publishing"),
    PUBLISHED("Published"),
    FAILED("Failed"),
    CANCELLED("Cancelled");

    private final String displayName;

    PostChannelStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Check if the status represents a successful publication
     */
    public boolean isPublished() {
        return this == PUBLISHED;
    }

    /**
     * Check if the status represents a final state
     */
    public boolean isFinal() {
        return this == PUBLISHED || this == FAILED || this == CANCELLED;
    }

    /**
     * Check if the status allows retry
     */
    public boolean canRetry() {
        return this == FAILED;
    }
}
