package fr.postiqa.core.infrastructure.client.actor.impl;

import fr.postiqa.core.domain.enums.SocialPlatform;
import fr.postiqa.core.domain.model.PostMetrics;
import fr.postiqa.core.domain.model.ProfileMetrics;
import fr.postiqa.core.domain.model.SocialPost;
import fr.postiqa.core.domain.model.SocialProfile;
import fr.postiqa.core.infrastructure.client.actor.ActorOutputParser;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generic output parser for Apify actors
 * <p>
 * Attempts to parse output using common field names.
 * This is a fallback when no platform-specific parser is available.
 */
public class GenericActorOutputParser implements ActorOutputParser {

    private final SocialPlatform defaultPlatform;

    public GenericActorOutputParser() {
        this.defaultPlatform = SocialPlatform.LINKEDIN;
    }

    public GenericActorOutputParser(SocialPlatform platform) {
        this.defaultPlatform = platform;
    }

    @Override
    public List<SocialPost> parsePosts(List<Map<String, Object>> items) {
        return items.stream()
            .map(this::parsePost)
            .collect(Collectors.toList());
    }

    @Override
    public SocialProfile parseProfile(Map<String, Object> item) {
        return new SocialProfile(
            getStringValue(item, "userId", "id", "publicIdentifier"),
            getStringValue(item, "username", "handle", "screenName"),
            defaultPlatform,
            getStringValue(item, "displayName", "fullName", "name"),
            getStringValue(item, "bio", "description", "headline"),
            getStringValue(item, "avatarUrl", "profilePicture", "profileImageUrl"),
            getStringValue(item, "coverImageUrl", "bannerUrl", "backgroundPicture"),
            getStringValue(item, "profileUrl", "url"),
            parseProfileMetrics(item),
            parseTags(item),
            parseMetadata(item),
            LocalDateTime.now()
        );
    }

    private SocialPost parsePost(Map<String, Object> item) {
        return new SocialPost(
            getStringValue(item, "postId", "id", "tweetId"),
            defaultPlatform,
            getStringValue(item, "authorId", "userId"),
            getStringValue(item, "authorName", "author", "username"),
            getStringValue(item, "content", "text", "caption"),
            parseDateTime(getStringValue(item, "publishedAt", "postedAt", "createdAt")),
            parseMediaUrls(item),
            parseMediaType(item),
            parsePostMetrics(item),
            getStringValue(item, "postUrl", "url", "link"),
            List.of(),  // threadContent - empty for generic posts
            Map.of()    // metadata - empty
        );
    }

    private PostMetrics parsePostMetrics(Map<String, Object> data) {
        return PostMetrics.of(
            getIntegerValue(data, "likes", "likesCount", "favoritesCount"),
            getIntegerValue(data, "comments", "commentsCount", "repliesCount"),
            getIntegerValue(data, "shares", "sharesCount", "retweetsCount"),
            getIntegerValue(data, "views", "viewsCount", "impressions")
        );
    }

    private ProfileMetrics parseProfileMetrics(Map<String, Object> data) {
        return ProfileMetrics.of(
            getIntegerValue(data, "followers", "followersCount"),
            getIntegerValue(data, "following", "followingCount", "connectionsCount"),
            getIntegerValue(data, "totalPosts", "postsCount"),
            null,
            null
        );
    }

    @SuppressWarnings("unchecked")
    private List<String> parseMediaUrls(Map<String, Object> data) {
        // Try different possible field names
        for (String key : List.of("mediaUrls", "media", "images", "photos")) {
            Object mediaObj = data.get(key);
            if (mediaObj instanceof List) {
                List<?> list = (List<?>) mediaObj;
                return list.stream()
                    .map(item -> {
                        if (item instanceof String) {
                            return (String) item;
                        } else if (item instanceof Map) {
                            return getStringValue((Map<String, Object>) item, "url", "src");
                        }
                        return null;
                    })
                    .filter(url -> url != null && !url.isEmpty())
                    .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

    private SocialPost.MediaType parseMediaType(Map<String, Object> data) {
        String type = getStringValue(data, "mediaType", "type");
        if (type == null) {
            // Infer from media URLs
            List<String> urls = parseMediaUrls(data);
            return urls.isEmpty() ? SocialPost.MediaType.NONE : SocialPost.MediaType.IMAGE;
        }

        return switch (type.toUpperCase()) {
            case "IMAGE", "PHOTO", "PICTURE" -> SocialPost.MediaType.IMAGE;
            case "VIDEO", "GIF" -> SocialPost.MediaType.VIDEO;
            case "CAROUSEL", "ALBUM" -> SocialPost.MediaType.CAROUSEL;
            case "DOCUMENT", "PDF", "FILE" -> SocialPost.MediaType.DOCUMENT;
            default -> SocialPost.MediaType.NONE;
        };
    }

    @SuppressWarnings("unchecked")
    private List<String> parseTags(Map<String, Object> data) {
        Object tagsObj = data.get("tags");
        if (tagsObj instanceof List) {
            return (List<String>) tagsObj;
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseMetadata(Map<String, Object> data) {
        Object metadataObj = data.get("metadata");
        if (metadataObj instanceof Map) {
            return (Map<String, String>) metadataObj;
        }
        return Collections.emptyMap();
    }

    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(dateStr);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    // Helper methods to try multiple field names
    private String getStringValue(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }

    private Integer getIntegerValue(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
        }
        return null;
    }
}
