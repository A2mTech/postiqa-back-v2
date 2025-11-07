package fr.postiqa.core.infrastructure.client.actor.impl;

import fr.postiqa.core.infrastructure.client.actor.ActorInputBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Input builder for TikTok ScrapTik actor
 * <p>
 * Builds input for the scraptik~tiktok-api actor.
 * ScrapTik uses specific field names for different endpoints.
 */
public class TikTokActorInputBuilder implements ActorInputBuilder {

    @Override
    public Map<String, Object> buildPostsInput(String userId, Integer maxItems) {
        Map<String, Object> input = new HashMap<>();

        // TikTok uses 'username' for profile-based scraping
        // Username should NOT include the @ symbol
        input.put("username", removeAtSymbol(userId));

        // For user posts endpoint
        input.put("count", maxItems != null ? maxItems : 10);
        input.put("max_cursor", "0"); // Start from beginning

        return input;
    }

    @Override
    public Map<String, Object> buildProfileInput(String userId) {
        Map<String, Object> input = new HashMap<>();

        // TikTok profile endpoint uses 'username' (without @)
        input.put("username", removeAtSymbol(userId));

        return input;
    }

    @Override
    public Map<String, Object> buildCustomInput(String userId, Map<String, Object> additionalParams) {
        Map<String, Object> input = new HashMap<>();

        // Base user identifier
        input.put("username", removeAtSymbol(userId));

        // Merge additional parameters
        input.putAll(additionalParams);

        return input;
    }

    /**
     * Build input for fetching user posts with pagination
     */
    public Map<String, Object> buildUserPostsInput(String userId, Integer count, String cursor) {
        Map<String, Object> input = new HashMap<>();

        input.put("username", removeAtSymbol(userId));
        input.put("count", count != null ? count : 10);
        input.put("max_cursor", cursor != null ? cursor : "0");

        return input;
    }

    /**
     * Build input for searching posts by keyword
     */
    public Map<String, Object> buildSearchPostsInput(String keyword, Integer count, Integer offset) {
        Map<String, Object> input = new HashMap<>();

        input.put("keyword", keyword);
        input.put("count", count != null ? count : 10);
        input.put("offset", offset != null ? offset : 0);
        input.put("use_filters", false);
        input.put("publish_time", 0); // All time
        input.put("sort_type", 0); // Relevance

        return input;
    }

    /**
     * Build input for searching users by keyword
     */
    public Map<String, Object> buildSearchUsersInput(String keyword, Integer count) {
        Map<String, Object> input = new HashMap<>();

        input.put("keyword", keyword);
        input.put("count", count != null ? count : 20);
        input.put("cursor", 0);

        return input;
    }

    /**
     * Build input for getting video details
     */
    public Map<String, Object> buildVideoDetailsInput(String awemeId) {
        Map<String, Object> input = new HashMap<>();
        input.put("aweme_id", awemeId);
        return input;
    }

    /**
     * Remove @ symbol from username if present
     */
    private String removeAtSymbol(String username) {
        if (username == null) {
            return null;
        }
        return username.startsWith("@") ? username.substring(1) : username;
    }
}
