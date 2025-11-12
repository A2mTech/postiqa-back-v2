package fr.postiqa.core.domain.model;

import fr.postiqa.core.domain.enums.PostType;
import fr.postiqa.core.domain.enums.SocialPlatform;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    String postUrl,
    List<String> threadContent,  // For Twitter threads
    Map<String, Object> metadata  // Additional platform-specific data
) {
    public SocialPost {
        // Ensure mediaUrls is never null
        mediaUrls = mediaUrls != null ? List.copyOf(mediaUrls) : Collections.emptyList();
        // Default metrics if null
        metrics = metrics != null ? metrics : PostMetrics.empty();
        // Ensure threadContent is never null
        threadContent = threadContent != null ? List.copyOf(threadContent) : Collections.emptyList();
        // Ensure metadata is never null
        metadata = metadata != null ? Map.copyOf(metadata) : Collections.emptyMap();
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

    /**
     * Get PostType for content analysis workflows
     */
    public PostType postType() {
        if (!threadContent.isEmpty()) {
            return PostType.THREAD;
        }

        return switch (mediaType) {
            case NONE -> PostType.TEXT;
            case IMAGE -> mediaUrls.size() > 1 ? PostType.CAROUSEL : PostType.IMAGE;
            case VIDEO -> PostType.VIDEO;
            case CAROUSEL -> PostType.CAROUSEL;
            case DOCUMENT -> PostType.TEXT;  // Treat documents as text posts
        };
    }

    /**
     * Get engagement metrics as a map for backward compatibility
     */
    public Map<String, Integer> engagementMetrics() {
        Map<String, Integer> map = new HashMap<>();
        if (metrics != null) {
            map.put("likes", metrics.likes() != null ? metrics.likes() : 0);
            map.put("comments", metrics.comments() != null ? metrics.comments() : 0);
            map.put("shares", metrics.shares() != null ? metrics.shares() : 0);
            map.put("views", metrics.views() != null ? metrics.views() : 0);
            map.put("retweets", metrics.shares() != null ? metrics.shares() : 0);  // Alias for Twitter
            map.put("replies", metrics.comments() != null ? metrics.comments() : 0);  // Alias for Twitter
        }
        return map;
    }

    public enum MediaType {
        NONE,
        IMAGE,
        VIDEO,
        CAROUSEL,
        DOCUMENT
    }
}
