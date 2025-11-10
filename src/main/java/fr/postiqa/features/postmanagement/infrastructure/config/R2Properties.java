package fr.postiqa.features.postmanagement.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Cloudflare R2 storage.
 * R2 is S3-compatible, so we use the AWS SDK.
 */
@ConfigurationProperties(prefix = "storage.r2")
public record R2Properties(
    String endpoint,
    String bucket,
    String accessKey,
    String secretKey,
    String cdnUrl,
    String region
) {
    public R2Properties {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("R2 endpoint cannot be null or blank");
        }
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("R2 bucket cannot be null or blank");
        }
        if (accessKey == null || accessKey.isBlank()) {
            throw new IllegalArgumentException("R2 access key cannot be null or blank");
        }
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalArgumentException("R2 secret key cannot be null or blank");
        }
        // Default region for R2
        region = region != null && !region.isBlank() ? region : "auto";
    }
}
