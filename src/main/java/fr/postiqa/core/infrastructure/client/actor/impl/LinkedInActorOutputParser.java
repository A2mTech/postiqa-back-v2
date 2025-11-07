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
 * Output parser for LinkedIn Apify actors
 * <p>
 * Parses the actual LinkedIn scraper API response format:
 * {
 *   "success": true,
 *   "data": {
 *     "posts": [ ... ],
 *     "pagination_token": "..."
 *   }
 * }
 */
public class LinkedInActorOutputParser implements ActorOutputParser {

    @Override
    @SuppressWarnings("unchecked")
    public List<SocialPost> parsePosts(List<Map<String, Object>> items) {
        List<SocialPost> allPosts = new ArrayList<>();

        for (Map<String, Object> item : items) {
            // Check if this is the wrapper response
            Object dataObj = item.get("data");
            if (dataObj instanceof Map) {
                Map<String, Object> data = (Map<String, Object>) dataObj;
                Object postsObj = data.get("posts");

                if (postsObj instanceof List) {
                    List<Map<String, Object>> posts = (List<Map<String, Object>>) postsObj;
                    for (Map<String, Object> post : posts) {
                        allPosts.add(parseLinkedInPost(post));
                    }
                }
            } else {
                // Fallback: might be a direct post object
                allPosts.add(parseLinkedInPost(item));
            }
        }

        return allPosts;
    }

    @Override
    public SocialProfile parseProfile(Map<String, Object> item) {
        // For profile scraping, extract author info from first post or dedicated profile endpoint
        Object dataObj = item.get("data");
        if (dataObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) dataObj;

            // Try to get profile from author of first post
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> posts = (List<Map<String, Object>>) data.get("posts");
            if (posts != null && !posts.isEmpty()) {
                Map<String, Object> firstPost = posts.get(0);
                @SuppressWarnings("unchecked")
                Map<String, Object> author = (Map<String, Object>) firstPost.get("author");

                if (author != null) {
                    return parseProfileFromAuthor(author, posts.size());
                }
            }
        }

        // Fallback to generic parsing
        return parseProfileFromAuthor(item, 0);
    }

    private SocialPost parseLinkedInPost(Map<String, Object> postData) {
        // Extract author info
        @SuppressWarnings("unchecked")
        Map<String, Object> author = (Map<String, Object>) postData.get("author");

        String authorId = author != null ? getString(author, "username") : null;
        String authorName = author != null
            ? getString(author, "first_name") + " " + getString(author, "last_name")
            : "Unknown";

        // Extract posted_at info
        @SuppressWarnings("unchecked")
        Map<String, Object> postedAt = (Map<String, Object>) postData.get("posted_at");
        LocalDateTime publishedDate = parseLinkedInDateTime(postedAt);

        // Extract stats
        @SuppressWarnings("unchecked")
        Map<String, Object> stats = (Map<String, Object>) postData.get("stats");
        PostMetrics metrics = parseLinkedInStats(stats);

        // Extract media
        List<String> mediaUrls = new ArrayList<>();
        SocialPost.MediaType mediaType = SocialPost.MediaType.NONE;

        Object mediaObj = postData.get("media");
        if (mediaObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> media = (Map<String, Object>) mediaObj;
            String type = getString(media, "type");
            String url = getString(media, "url");

            if (url != null) {
                mediaUrls.add(url);
            }

            // Check for multiple images
            Object imagesObj = media.get("images");
            if (imagesObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> images = (List<Map<String, Object>>) imagesObj;
                for (Map<String, Object> image : images) {
                    String imageUrl = getString(image, "url");
                    if (imageUrl != null && !mediaUrls.contains(imageUrl)) {
                        mediaUrls.add(imageUrl);
                    }
                }
            }

            mediaType = parseMediaType(type, media);
        }

        // Handle article
        Object articleObj = postData.get("article");
        if (articleObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> article = (Map<String, Object>) articleObj;
            String thumbnail = getString(article, "thumbnail");
            if (thumbnail != null) {
                mediaUrls.add(thumbnail);
            }
            mediaType = SocialPost.MediaType.DOCUMENT; // Articles are treated as documents
        }

        // Handle document
        Object documentObj = postData.get("document");
        if (documentObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> document = (Map<String, Object>) documentObj;
            String thumbnail = getString(document, "thumbnail");
            if (thumbnail != null) {
                mediaUrls.add(thumbnail);
            }
            mediaType = SocialPost.MediaType.DOCUMENT;
        }

        return new SocialPost(
            getString(postData, "urn"),
            SocialPlatform.LINKEDIN,
            authorId,
            authorName,
            getString(postData, "text"),
            publishedDate,
            mediaUrls,
            mediaType,
            metrics,
            getString(postData, "url")
        );
    }

    private SocialProfile parseProfileFromAuthor(Map<String, Object> author, int totalPosts) {
        String firstName = getString(author, "first_name");
        String lastName = getString(author, "last_name");
        String fullName = (firstName != null && lastName != null)
            ? firstName + " " + lastName
            : getString(author, "username");

        return new SocialProfile(
            getString(author, "username"),
            getString(author, "username"),
            SocialPlatform.LINKEDIN,
            fullName,
            getString(author, "headline"),
            getString(author, "profile_picture"),
            null, // Cover image not provided in this format
            getString(author, "profile_url"),
            ProfileMetrics.of(null, null, totalPosts, null, null),
            Collections.emptyList(),
            Collections.emptyMap(),
            LocalDateTime.now()
        );
    }

    private LocalDateTime parseLinkedInDateTime(Map<String, Object> postedAt) {
        if (postedAt == null) {
            return null;
        }

        // Try timestamp first (most accurate)
        Object timestamp = postedAt.get("timestamp");
        if (timestamp instanceof Number) {
            long millis = ((Number) timestamp).longValue();
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
        }

        // Fallback to date string
        String dateStr = getString(postedAt, "date");
        if (dateStr != null) {
            try {
                // Format: "2025-05-15 14:30:20"
                return LocalDateTime.parse(dateStr.replace(" ", "T"));
            } catch (Exception e) {
                // Ignore parsing errors
            }
        }

        return null;
    }

    private PostMetrics parseLinkedInStats(Map<String, Object> stats) {
        if (stats == null) {
            return PostMetrics.empty();
        }

        Integer totalReactions = getInteger(stats, "total_reactions");
        Integer comments = getInteger(stats, "comments");
        Integer reposts = getInteger(stats, "reposts");

        return PostMetrics.of(
            totalReactions,
            comments,
            reposts,
            null // Views not provided
        );
    }

    private SocialPost.MediaType parseMediaType(String type, Map<String, Object> media) {
        if (type == null) {
            return SocialPost.MediaType.NONE;
        }

        return switch (type.toLowerCase()) {
            case "image" -> SocialPost.MediaType.IMAGE;
            case "images" -> {
                // Multiple images = carousel
                Object imagesObj = media.get("images");
                if (imagesObj instanceof List && ((List<?>) imagesObj).size() > 1) {
                    yield SocialPost.MediaType.CAROUSEL;
                }
                yield SocialPost.MediaType.IMAGE;
            }
            case "video" -> SocialPost.MediaType.VIDEO;
            case "document" -> SocialPost.MediaType.DOCUMENT;
            default -> SocialPost.MediaType.NONE;
        };
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
}
