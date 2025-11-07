package fr.postiqa.core.infrastructure.client.actor.impl;

import fr.postiqa.core.infrastructure.client.actor.ActorInputBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic input builder for Apify actors
 * <p>
 * Uses a generic format that works with most social media scraping actors.
 * This is a fallback when no platform-specific builder is available.
 */
public class GenericActorInputBuilder implements ActorInputBuilder {

    @Override
    public Map<String, Object> buildPostsInput(String userId, Integer maxItems) {
        Map<String, Object> input = new HashMap<>();

        input.put("userId", userId);
        input.put("username", userId);
        input.put("maxPosts", maxItems != null ? maxItems : 50);
        input.put("maxItems", maxItems != null ? maxItems : 50);
        input.put("includeMetrics", true);
        input.put("includeMedia", true);

        return input;
    }

    @Override
    public Map<String, Object> buildProfileInput(String userId) {
        Map<String, Object> input = new HashMap<>();

        input.put("userId", userId);
        input.put("username", userId);
        input.put("includeMetrics", true);
        input.put("includeFollowers", true);
        input.put("includeStats", true);

        return input;
    }

    @Override
    public Map<String, Object> buildCustomInput(String userId, Map<String, Object> additionalParams) {
        Map<String, Object> input = new HashMap<>();
        input.put("userId", userId);
        input.put("username", userId);
        input.putAll(additionalParams);
        return input;
    }
}
