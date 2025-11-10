package fr.postiqa.shared.enums;

/**
 * Type of media attached to a post.
 */
public enum MediaType {
    IMAGE("Image", new String[]{"image/jpeg", "image/png", "image/gif", "image/webp"}, 10 * 1024 * 1024), // 10MB
    VIDEO("Video", new String[]{"video/mp4", "video/quicktime", "video/x-msvideo"}, 100 * 1024 * 1024), // 100MB
    DOCUMENT("Document", new String[]{"application/pdf"}, 25 * 1024 * 1024); // 25MB

    private final String displayName;
    private final String[] allowedMimeTypes;
    private final long maxSizeBytes;

    MediaType(String displayName, String[] allowedMimeTypes, long maxSizeBytes) {
        this.displayName = displayName;
        this.allowedMimeTypes = allowedMimeTypes;
        this.maxSizeBytes = maxSizeBytes;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String[] getAllowedMimeTypes() {
        return allowedMimeTypes;
    }

    public long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    /**
     * Check if a MIME type is allowed for this media type
     */
    public boolean isAllowedMimeType(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        for (String allowed : allowedMimeTypes) {
            if (mimeType.equalsIgnoreCase(allowed)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a file size is within the limit
     */
    public boolean isValidSize(long sizeBytes) {
        return sizeBytes > 0 && sizeBytes <= maxSizeBytes;
    }

    /**
     * Get media type from MIME type
     */
    public static MediaType fromMimeType(String mimeType) {
        if (mimeType == null) {
            return null;
        }
        for (MediaType type : values()) {
            if (type.isAllowedMimeType(mimeType)) {
                return type;
            }
        }
        return null;
    }
}
