package fr.postiqa.core.domain.model;

import fr.postiqa.core.domain.enums.ContentType;
import fr.postiqa.core.domain.enums.ExecutionMode;
import fr.postiqa.core.domain.enums.SocialPlatform;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a scraping request with all necessary parameters
 */
public record ScrapingRequest(
    SocialPlatform platform,
    String userId,
    String url,
    ContentType contentType,
    Integer maxItems,
    ExecutionMode executionMode,
    Map<String, Object> additionalParams
) {
    public ScrapingRequest {
        // Ensure additionalParams is never null
        additionalParams = additionalParams != null ? Map.copyOf(additionalParams) : Collections.emptyMap();
        // Default execution mode
        executionMode = executionMode != null ? executionMode : ExecutionMode.SYNC;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Generate a cache key for this request
     */
    public String cacheKey() {
        if (platform != null) {
            // Social media scraping
            return String.format("social:%s:%s:%d", platform, userId, maxItems != null ? maxItems : 0);
        } else if (url != null) {
            // Web scraping
            return String.format("web:%s:%s", contentType, url);
        }
        return "unknown";
    }

    public boolean isSocialMedia() {
        return platform != null;
    }

    public boolean isWebScraping() {
        return url != null;
    }

    public static class Builder {
        private SocialPlatform platform;
        private String userId;
        private String url;
        private ContentType contentType;
        private Integer maxItems;
        private ExecutionMode executionMode = ExecutionMode.SYNC;
        private Map<String, Object> additionalParams = Collections.emptyMap();

        public Builder platform(SocialPlatform platform) {
            this.platform = platform;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder contentType(ContentType contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder maxItems(Integer maxItems) {
            this.maxItems = maxItems;
            return this;
        }

        public Builder executionMode(ExecutionMode executionMode) {
            this.executionMode = executionMode;
            return this;
        }

        public Builder additionalParams(Map<String, Object> additionalParams) {
            this.additionalParams = additionalParams;
            return this;
        }

        public ScrapingRequest build() {
            return new ScrapingRequest(
                platform,
                userId,
                url,
                contentType,
                maxItems,
                executionMode,
                additionalParams
            );
        }
    }
}
