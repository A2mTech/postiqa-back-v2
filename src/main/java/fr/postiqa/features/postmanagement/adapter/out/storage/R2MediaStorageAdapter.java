package fr.postiqa.features.postmanagement.adapter.out.storage;

import fr.postiqa.features.postmanagement.domain.exception.MediaUploadException;
import fr.postiqa.features.postmanagement.domain.port.MediaStoragePort;
import fr.postiqa.features.postmanagement.infrastructure.config.R2Properties;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

/**
 * R2 storage adapter implementing MediaStoragePort.
 * Uses AWS S3 SDK (R2 is S3-compatible).
 */
@Component
public class R2MediaStorageAdapter implements MediaStoragePort {

    private final S3Client s3Client;
    private final R2Properties r2Properties;

    public R2MediaStorageAdapter(S3Client s3Client, R2Properties r2Properties) {
        this.s3Client = s3Client;
        this.r2Properties = r2Properties;
    }

    @Override
    public UploadResult upload(byte[] fileBytes, UploadContext context, String mimeType) {
        String storageKey = generateStorageKey(context);

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(r2Properties.bucket())
                .key(storageKey)
                .contentType(mimeType)
                .contentLength((long) fileBytes.length)
                .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(fileBytes));

            String publicUrl = generatePublicUrl(storageKey);

            return new UploadResult(storageKey, publicUrl);
        } catch (S3Exception e) {
            throw new MediaUploadException(
                context.fileName(),
                "Failed to upload to R2: " + e.awsErrorDetails().errorMessage(),
                e
            );
        } catch (Exception e) {
            throw new MediaUploadException(
                context.fileName(),
                "Unexpected error during upload: " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(r2Properties.bucket())
                .key(storageKey)
                .build();

            s3Client.deleteObject(deleteRequest);
        } catch (S3Exception e) {
            // Log but don't throw - deletion failures shouldn't break the operation
            System.err.println("Failed to delete from R2: " + storageKey + " - " + e.awsErrorDetails().errorMessage());
        }
    }

    @Override
    public String getPublicUrl(String storageKey) {
        return generatePublicUrl(storageKey);
    }

    @Override
    public boolean exists(String storageKey) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                .bucket(r2Properties.bucket())
                .key(storageKey)
                .build();

            s3Client.headObject(headRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            System.err.println("Error checking file existence: " + storageKey + " - " + e.awsErrorDetails().errorMessage());
            return false;
        }
    }

    /**
     * Generate storage key with organization-based path
     * Format: {organizationId}/{mediaId}/{filename}
     */
    private String generateStorageKey(UploadContext context) {
        return String.format(
            "%s/%s/%s",
            context.organizationId().value(),
            context.mediaId().value(),
            sanitizeFileName(context.fileName())
        );
    }

    /**
     * Generate public URL for a storage key
     */
    private String generatePublicUrl(String storageKey) {
        if (r2Properties.cdnUrl() != null && !r2Properties.cdnUrl().isBlank()) {
            // Use CDN URL if configured
            return r2Properties.cdnUrl() + "/" + storageKey;
        } else {
            // Use R2 public bucket URL
            return r2Properties.endpoint() + "/" + r2Properties.bucket() + "/" + storageKey;
        }
    }

    /**
     * Sanitize filename for storage
     */
    private String sanitizeFileName(String fileName) {
        // Remove special characters and spaces
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
