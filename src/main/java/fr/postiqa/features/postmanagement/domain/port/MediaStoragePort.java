package fr.postiqa.features.postmanagement.domain.port;

import fr.postiqa.features.postmanagement.domain.vo.Media;
import fr.postiqa.features.postmanagement.domain.vo.MediaId;
import fr.postiqa.features.postmanagement.domain.vo.OrganizationId;

/**
 * Port for media storage operations (R2, S3, etc.).
 * Implemented by storage adapters.
 */
public interface MediaStoragePort {

    /**
     * Result of a media upload operation
     */
    record UploadResult(String storageKey, String publicUrl) {}

    /**
     * Context for media upload (for path generation)
     */
    record UploadContext(
        OrganizationId organizationId,
        MediaId mediaId,
        String fileName
    ) {}

    /**
     * Upload a media file
     *
     * @param fileBytes The file content
     * @param context Upload context (org, post, filename)
     * @param mimeType MIME type of the file
     * @return Upload result with storage key and public URL
     */
    UploadResult upload(byte[] fileBytes, UploadContext context, String mimeType);

    /**
     * Delete a media file
     *
     * @param storageKey The storage key of the file to delete
     */
    void delete(String storageKey);

    /**
     * Get public URL for a storage key
     *
     * @param storageKey The storage key
     * @return The public URL
     */
    String getPublicUrl(String storageKey);

    /**
     * Check if a file exists in storage
     *
     * @param storageKey The storage key
     * @return true if file exists
     */
    boolean exists(String storageKey);
}
