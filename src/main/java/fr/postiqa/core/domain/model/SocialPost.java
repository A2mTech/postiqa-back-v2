package fr.postiqa.core.domain.model;

import fr.postiqa.core.domain.enums.SocialPlatform;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Represents a social media post scraped from a platform
 */
public record SocialPost(
    String postId,
    SocialPlatform platform,
    String authorId,
    String authorName,
    String content,
    LocalDateTime publishedAt,
    List<String> mediaUrls,
    MediaType mediaType,
    PostMetrics metrics,
    String postUrl
) {
    public SocialPost {
        // Ensure mediaUrls is never null
        mediaUrls = mediaUrls != null ? List.copyOf(mediaUrls) : Collections.emptyList();
        // Default metrics if null
        metrics = metrics != null ? metrics : PostMetrics.empty();
    }

    public boolean hasMedia() {
        return !mediaUrls.isEmpty();
    }

    public boolean isVideo() {
        return mediaType == MediaType.VIDEO;
    }

    public boolean hasText() {
        return content != null && !content.isBlank();
    }

    public enum MediaType {
        NONE,
        IMAGE,
        VIDEO,
        CAROUSEL,
        DOCUMENT
    }
}
