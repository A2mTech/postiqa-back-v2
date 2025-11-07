package fr.postiqa.core.infrastructure.client.actor.impl;

import fr.postiqa.core.domain.enums.SocialPlatform;
import fr.postiqa.core.domain.model.PostMetrics;
import fr.postiqa.core.domain.model.ProfileMetrics;
import fr.postiqa.core.domain.model.SocialPost;
import fr.postiqa.core.domain.model.SocialProfile;
import fr.postiqa.core.infrastructure.client.actor.ActorOutputParser;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Output parser for Instagram Apify actor (apidojo/instagram-scraper)
 * <p>
 * Parses output from the Instagram Scraper actor.
 * Instagram response structure:
 * {
 *   "id": "...",
 *   "code": "...",
 *   "url": "...",
 *   "createdAt": "2023-11-27T07:48:34.000Z",
 *   "likeCount": 114,
 *   "commentCount": 5,
 *   "caption": "...",
 *   "owner": { ... },
 *   "location": { ... },
 *   "isCarousel": false,
 *   "isVideo": true,
 *   "video": { ... },
 *   "image": { ... },
 *   "audio": { ... }
 * }
 */
public class InstagramActorOutputParser implements ActorOutputParser {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    @Override
    @SuppressWarnings("unchecked")
    public List<SocialPost> parsePosts(List<Map<String, Object>> items) {
        List<SocialPost> allPosts = new ArrayList<>();

        for (Map<String, Object> item : items) {
            // Each item is an Instagram post
            allPosts.add(parseInstagramPost(item));
        }

        return allPosts;
    }

    @Override
    @SuppressWarnings("unchecked")
    public SocialProfile parseProfile(Map<String, Object> item) {
        // Instagram profile info is in the "owner" field of posts
        Object ownerObj = item.get("owner");
        if (ownerObj instanceof Map) {
            Map<String, Object> owner = (Map<String, Object>) ownerObj;
            return parseInstagramProfile(owner);
        }

        // Fallback: if item is already owner object
        return parseInstagramProfile(item);
    }

    @SuppressWarnings("unchecked")
    private SocialPost parseInstagramPost(Map<String, Object> postData) {
        // Extract owner info
        Map<String, Object> owner = null;
        Object ownerObj = postData.get("owner");
        if (ownerObj instanceof Map) {
            owner = (Map<String, Object>) ownerObj;
        }

        String authorId = owner != null ? getString(owner, "id") : null;
        String authorUsername = owner != null ? getString(owner, "username") : "Unknown";
        String authorName = owner != null ? getString(owner, "fullName") : authorUsername;

        // Extract post content
        String caption = getString(postData, "caption");
        String postUrl = getString(postData, "url");
        String postId = getString(postData, "id");
        String code = getString(postData, "code"); // Short code for post

        // Extract timestamp (ISO format: "2023-11-27T07:48:34.000Z")
        LocalDateTime publishedDate = parseInstagramDateTime(getString(postData, "createdAt"));

        // Extract metrics
        PostMetrics metrics = parseInstagramMetrics(postData);

        // Extract media
        List<String> mediaUrls = new ArrayList<>();
        SocialPost.MediaType mediaType = determineMediaType(postData, mediaUrls);

        return new SocialPost(
            postId,
            SocialPlatform.INSTAGRAM,
            authorId,
            authorName,
            caption,
            publishedDate,
            mediaUrls,
            mediaType,
            metrics,
            postUrl
        );
    }

    @SuppressWarnings("unchecked")
    private SocialProfile parseInstagramProfile(Map<String, Object> ownerData) {
        String userId = getString(ownerData, "id");
        String username = getString(ownerData, "username");
        String fullName = getString(ownerData, "fullName");
        String profilePicUrl = getString(ownerData, "profilePicUrl");

        // Profile URL
        String profileUrl = username != null
            ? String.format("https://www.instagram.com/%s/", username)
            : null;

        // Metrics
        Integer followerCount = getInteger(ownerData, "followerCount");
        Integer followingCount = getInteger(ownerData, "followingCount");
        Integer postCount = getInteger(ownerData, "postCount");

        ProfileMetrics metrics = ProfileMetrics.of(
            followerCount,
            followingCount,
            postCount,
            null,
            null
        );

        // Additional info
        Boolean isPrivate = getBoolean(ownerData, "isPrivate");
        Boolean isVerified = getBoolean(ownerData, "isVerified");

        Map<String, String> metadata = Map.of(
            "is_private", String.valueOf(isPrivate),
            "is_verified", String.valueOf(isVerified)
        );

        return new SocialProfile(
            userId,
            username,
            SocialPlatform.INSTAGRAM,
            fullName,
            null, // No bio in basic owner object
            profilePicUrl,
            null, // No cover image
            profileUrl,
            metrics,
            Collections.emptyList(),
            metadata,
            LocalDateTime.now()
        );
    }

    @SuppressWarnings("unchecked")
    private SocialPost.MediaType determineMediaType(Map<String, Object> postData, List<String> mediaUrls) {
        // Check if carousel (multiple images)
        Boolean isCarousel = getBoolean(postData, "isCarousel");
        if (isCarousel != null && isCarousel) {
            // Add image URL if available
            Object imageObj = postData.get("image");
            if (imageObj instanceof Map) {
                Map<String, Object> image = (Map<String, Object>) imageObj;
                String imageUrl = getString(image, "url");
                if (imageUrl != null) {
                    mediaUrls.add(imageUrl);
                }
            }
            return SocialPost.MediaType.CAROUSEL;
        }

        // Check if video
        Boolean isVideo = getBoolean(postData, "isVideo");
        if (isVideo != null && isVideo) {
            // Add video URL
            Object videoObj = postData.get("video");
            if (videoObj instanceof Map) {
                Map<String, Object> video = (Map<String, Object>) videoObj;
                String videoUrl = getString(video, "url");
                if (videoUrl != null) {
                    mediaUrls.add(videoUrl);
                }
            }

            // Also add thumbnail
            Object imageObj = postData.get("image");
            if (imageObj instanceof Map) {
                Map<String, Object> image = (Map<String, Object>) imageObj;
                String thumbnailUrl = getString(image, "url");
                if (thumbnailUrl != null) {
                    mediaUrls.add(thumbnailUrl);
                }
            }

            return SocialPost.MediaType.VIDEO;
        }

        // Default: single image
        Object imageObj = postData.get("image");
        if (imageObj instanceof Map) {
            Map<String, Object> image = (Map<String, Object>) imageObj;
            String imageUrl = getString(image, "url");
            if (imageUrl != null) {
                mediaUrls.add(imageUrl);
                return SocialPost.MediaType.IMAGE;
            }
        }

        return SocialPost.MediaType.NONE;
    }

    private PostMetrics parseInstagramMetrics(Map<String, Object> postData) {
        Integer likes = getInteger(postData, "likeCount");
        Integer comments = getInteger(postData, "commentCount");

        // Video play count (for videos)
        Integer views = null;
        Object videoObj = postData.get("video");
        if (videoObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> video = (Map<String, Object>) videoObj;
            views = getInteger(video, "playCount");
        }

        return PostMetrics.of(likes, comments, null, views);
    }

    private LocalDateTime parseInstagramDateTime(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        try {
            // Parse ISO 8601 format: "2023-11-27T07:48:34.000Z"
            Instant instant = Instant.from(ISO_FORMATTER.parse(dateStr));
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        } catch (Exception e) {
            // Fallback: try as epoch timestamp
            try {
                long timestamp = Long.parseLong(dateStr);
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
            } catch (Exception ex) {
                return null;
            }
        }
    }

    // Helper methods
    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private Integer getInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    private Boolean getBoolean(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return null;
    }
}
