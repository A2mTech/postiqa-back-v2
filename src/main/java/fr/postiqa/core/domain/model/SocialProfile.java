package fr.postiqa.core.domain.model;

import fr.postiqa.core.domain.enums.SocialPlatform;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents a social media profile scraped from a platform
 */
public record SocialProfile(
    String userId,
    String username,
    SocialPlatform platform,
    String displayName,
    String bio,
    String avatarUrl,
    String coverImageUrl,
    String profileUrl,
    ProfileMetrics metrics,
    List<String> tags,
    Map<String, String> metadata,
    LocalDateTime scrapedAt
) {
    public SocialProfile {
        // Ensure collections are never null
        tags = tags != null ? List.copyOf(tags) : Collections.emptyList();
        metadata = metadata != null ? Map.copyOf(metadata) : Collections.emptyMap();
        metrics = metrics != null ? metrics : ProfileMetrics.empty();
        scrapedAt = scrapedAt != null ? scrapedAt : LocalDateTime.now();
    }

    public boolean hasBio() {
        return bio != null && !bio.isBlank();
    }

    public boolean hasAvatar() {
        return avatarUrl != null && !avatarUrl.isBlank();
    }

    public boolean isVerified() {
        return metadata.containsKey("verified") && Boolean.parseBoolean(metadata.get("verified"));
    }
}
