package fr.postiqa.features.postmanagement.domain.vo;

import fr.postiqa.shared.enums.MediaType;

/**
 * Value object representing a media file attached to a post.
 * Contains metadata about the media file stored in external storage.
 */
public record Media(
    MediaId id,
    String storageKey,
    String publicUrl,
    String fileName,
    String mimeType,
    MediaType type,
    long fileSize,
    Integer width,
    Integer height,
    Integer durationSeconds
) {

    public Media {
        if (id == null) {
            throw new IllegalArgumentException("Media ID cannot be null");
        }
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("Storage key cannot be null or blank");
        }
        if (publicUrl == null || publicUrl.isBlank()) {
            throw new IllegalArgumentException("Public URL cannot be null or blank");
        }
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("File name cannot be null or blank");
        }
        if (mimeType == null || mimeType.isBlank()) {
            throw new IllegalArgumentException("MIME type cannot be null or blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("Media type cannot be null");
        }
        if (fileSize <= 0) {
            throw new IllegalArgumentException("File size must be positive");
        }
        if (!type.isValidSize(fileSize)) {
            throw new IllegalArgumentException(
                "File size " + fileSize + " bytes exceeds maximum " + type.getMaxSizeBytes() + " bytes for " + type
            );
        }
        if (!type.isAllowedMimeType(mimeType)) {
            throw new IllegalArgumentException("MIME type " + mimeType + " not allowed for " + type);
        }
    }

    /**
     * Create a Media instance
     */
    public static Media create(
        MediaId id,
        String storageKey,
        String publicUrl,
        String fileName,
        String mimeType,
        MediaType type,
        long fileSize,
        Integer width,
        Integer height,
        Integer durationSeconds
    ) {
        return new Media(id, storageKey, publicUrl, fileName, mimeType, type, fileSize, width, height, durationSeconds);
    }

    /**
     * Create a Media instance for an image
     */
    public static Media image(
        MediaId id,
        String storageKey,
        String publicUrl,
        String fileName,
        String mimeType,
        long fileSize,
        int width,
        int height
    ) {
        return new Media(id, storageKey, publicUrl, fileName, mimeType, MediaType.IMAGE, fileSize, width, height, null);
    }

    /**
     * Create a Media instance for a video
     */
    public static Media video(
        MediaId id,
        String storageKey,
        String publicUrl,
        String fileName,
        String mimeType,
        long fileSize,
        int width,
        int height,
        int durationSeconds
    ) {
        return new Media(id, storageKey, publicUrl, fileName, mimeType, MediaType.VIDEO, fileSize, width, height, durationSeconds);
    }

    /**
     * Create a Media instance for a document
     */
    public static Media document(
        MediaId id,
        String storageKey,
        String publicUrl,
        String fileName,
        String mimeType,
        long fileSize
    ) {
        return new Media(id, storageKey, publicUrl, fileName, mimeType, MediaType.DOCUMENT, fileSize, null, null, null);
    }

    /**
     * Check if media is an image
     */
    public boolean isImage() {
        return type == MediaType.IMAGE;
    }

    /**
     * Check if media is a video
     */
    public boolean isVideo() {
        return type == MediaType.VIDEO;
    }

    /**
     * Check if media is a document
     */
    public boolean isDocument() {
        return type == MediaType.DOCUMENT;
    }

    /**
     * Check if media has dimensions (images/videos)
     */
    public boolean hasDimensions() {
        return width != null && height != null;
    }

    /**
     * Get file size in MB
     */
    public double getFileSizeMB() {
        return fileSize / (1024.0 * 1024.0);
    }
}
