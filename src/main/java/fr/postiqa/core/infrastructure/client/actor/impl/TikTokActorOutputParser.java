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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Output parser for TikTok ScrapTik actor
 * <p>
 * Parses output from the scraptik~tiktok-api actor.
 * TikTok response structure varies by endpoint but generally follows:
 * {
 *   "user": { ... },  // For profile endpoints
 *   "aweme_list": [ ... ],  // For video posts
 *   "data": { ... }  // Other endpoints
 * }
 */
public class TikTokActorOutputParser implements ActorOutputParser {

    @Override
    @SuppressWarnings("unchecked")
    public List<SocialPost> parsePosts(List<Map<String, Object>> items) {
        List<SocialPost> allPosts = new ArrayList<>();

        for (Map<String, Object> item : items) {
            // Check for aweme_list (videos array)
            Object awemeList = item.get("aweme_list");
            if (awemeList instanceof List) {
                List<Map<String, Object>> videos = (List<Map<String, Object>>) awemeList;
                for (Map<String, Object> video : videos) {
                    allPosts.add(parseTikTokVideo(video));
                }
                continue;
            }

            // Check if item itself is a video
            if (item.containsKey("aweme_id") || item.containsKey("video")) {
                allPosts.add(parseTikTokVideo(item));
            }
        }

        return allPosts;
    }

    @Override
    @SuppressWarnings("unchecked")
    public SocialProfile parseProfile(Map<String, Object> item) {
        // TikTok profile can be in "user" field or at root level
        Map<String, Object> userData = item;

        Object userObj = item.get("user");
        if (userObj instanceof Map) {
            userData = (Map<String, Object>) userObj;
        }

        return parseTikTokUser(userData);
    }

    @SuppressWarnings("unchecked")
    private SocialPost parseTikTokVideo(Map<String, Object> videoData) {
        // Extract author info
        Map<String, Object> author = null;
        Object authorObj = videoData.get("author");
        if (authorObj instanceof Map) {
            author = (Map<String, Object>) authorObj;
        }

        String authorId = author != null ? getString(author, "unique_id") : null;
        String authorName = author != null ? getString(author, "nickname") : "Unknown";

        // Extract video description
        String description = getString(videoData, "desc");

        // Extract statistics
        Map<String, Object> stats = null;
        Object statsObj = videoData.get("statistics");
        if (statsObj instanceof Map) {
            stats = (Map<String, Object>) statsObj;
        }

        PostMetrics metrics = parseTikTokStats(stats);

        // Extract video info
        Map<String, Object> video = null;
        Object videoObj = videoData.get("video");
        if (videoObj instanceof Map) {
            video = (Map<String, Object>) videoObj;
        }

        List<String> mediaUrls = new ArrayList<>();
        if (video != null) {
            // Cover/thumbnail
            String cover = getString(video, "cover");
            if (cover != null) {
                mediaUrls.add(cover);
            }

            // Play URL (actual video)
            Object playAddrObj = video.get("play_addr");
            if (playAddrObj instanceof Map) {
                Map<String, Object> playAddr = (Map<String, Object>) playAddrObj;
                Object urlListObj = playAddr.get("url_list");
                if (urlListObj instanceof List) {
                    List<String> urlList = (List<String>) urlListObj;
                    if (!urlList.isEmpty()) {
                        mediaUrls.add(urlList.get(0));
                    }
                }
            }
        }

        // Extract timestamp
        Object createTime = videoData.get("create_time");
        LocalDateTime publishedDate = null;
        if (createTime instanceof Number) {
            long timestamp = ((Number) createTime).longValue();
            publishedDate = LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());
        }

        // Build video URL
        String awemeId = getString(videoData, "aweme_id");
        String videoUrl = authorId != null && awemeId != null
            ? String.format("https://www.tiktok.com/@%s/video/%s", authorId, awemeId)
            : null;

        return new SocialPost(
            awemeId,
            SocialPlatform.TIKTOK,
            authorId,
            authorName,
            description,
            publishedDate,
            mediaUrls,
            SocialPost.MediaType.VIDEO, // TikTok is primarily video
            metrics,
            videoUrl,
            List.of(),  // threadContent - empty for TikTok videos
            Map.of()    // metadata - empty
        );
    }

    @SuppressWarnings("unchecked")
    private SocialProfile parseTikTokUser(Map<String, Object> userData) {
        String uniqueId = getString(userData, "unique_id"); // Username
        String nickname = getString(userData, "nickname"); // Display name
        String signature = getString(userData, "signature"); // Bio

        // Avatar URLs
        Object avatarObj = userData.get("avatar_larger");
        String avatarUrl = null;
        if (avatarObj instanceof Map) {
            Map<String, Object> avatar = (Map<String, Object>) avatarObj;
            Object urlListObj = avatar.get("url_list");
            if (urlListObj instanceof List) {
                List<String> urlList = (List<String>) urlListObj;
                if (!urlList.isEmpty()) {
                    avatarUrl = urlList.get(0);
                }
            }
        } else if (avatarObj instanceof String) {
            avatarUrl = (String) avatarObj;
        }

        // Profile URL
        String profileUrl = uniqueId != null
            ? String.format("https://www.tiktok.com/@%s", uniqueId)
            : null;

        // Metrics
        Integer followerCount = getInteger(userData, "follower_count");
        Integer followingCount = getInteger(userData, "following_count");
        Integer videoCount = getInteger(userData, "aweme_count");

        ProfileMetrics metrics = ProfileMetrics.of(
            followerCount,
            followingCount,
            videoCount,
            null,
            null
        );

        // Additional info
        Boolean isVerified = getBoolean(userData, "verified");
        Map<String, String> metadata = Map.of(
            "verified", String.valueOf(isVerified),
            "uid", getString(userData, "uid", "")
        );

        return new SocialProfile(
            getString(userData, "uid"), // User ID
            uniqueId, // Username
            SocialPlatform.TIKTOK,
            nickname, // Display name
            signature, // Bio
            avatarUrl,
            null, // No cover image
            profileUrl,
            metrics,
            Collections.emptyList(), // No tags in basic profile
            metadata,
            LocalDateTime.now()
        );
    }

    private PostMetrics parseTikTokStats(Map<String, Object> stats) {
        if (stats == null) {
            return PostMetrics.empty();
        }

        Integer likes = getInteger(stats, "digg_count");
        Integer comments = getInteger(stats, "comment_count");
        Integer shares = getInteger(stats, "share_count");
        Integer views = getInteger(stats, "play_count");

        return PostMetrics.of(likes, comments, shares, views);
    }

    // Helper methods
    private String getString(Map<String, Object> map, String key) {
        return getString(map, key, null);
    }

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
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
        return false;
    }
}
