package fr.postiqa.shared.enums;

/**
 * Origin/type of post creation.
 */
public enum PostType {
    MANUAL("Manual"),
    GENERATED("AI Generated"),
    IMPORTED("Imported");

    private final String displayName;

    PostType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
