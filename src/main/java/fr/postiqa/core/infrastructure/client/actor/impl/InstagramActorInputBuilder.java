package fr.postiqa.core.infrastructure.client.actor.impl;

import fr.postiqa.core.infrastructure.client.actor.ActorInputBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Input builder for Instagram Apify actor (apidojo/instagram-scraper)
 * <p>
 * Builds input for the Instagram Scraper actor which uses startUrls array.
 * Supports multiple URL types: profile, tag, location, audio, reels, tagged.
 * <p>
 * Important: Minimum 10 posts per request (actor requirement).
 */
public class InstagramActorInputBuilder implements ActorInputBuilder {

    private static final int MIN_ITEMS = 10;

    @Override
    public Map<String, Object> buildPostsInput(String userId, Integer maxItems) {
        Map<String, Object> input = new HashMap<>();

        // Instagram uses startUrls array for profile scraping
        String profileUrl = buildProfileUrl(userId);
        input.put("startUrls", List.of(profileUrl));

        // Ensure minimum 10 posts (actor requirement)
        int items = maxItems != null ? Math.max(maxItems, MIN_ITEMS) : MIN_ITEMS;
        input.put("maxItems", items);

        return input;
    }

    @Override
    public Map<String, Object> buildProfileInput(String userId) {
        Map<String, Object> input = new HashMap<>();

        // For profile info, we still need to scrape some posts
        String profileUrl = buildProfileUrl(userId);
        input.put("startUrls", List.of(profileUrl));

        // Get minimum posts to extract profile info
        input.put("maxItems", MIN_ITEMS);

        return input;
    }

    @Override
    public Map<String, Object> buildCustomInput(String userId, Map<String, Object> additionalParams) {
        Map<String, Object> input = new HashMap<>();

        // Base profile URL
        String profileUrl = buildProfileUrl(userId);
        input.put("startUrls", List.of(profileUrl));

        // Default max items
        input.put("maxItems", MIN_ITEMS);

        // Merge additional parameters
        input.putAll(additionalParams);

        return input;
    }

    /**
     * Build input for scraping posts from a hashtag
     */
    public Map<String, Object> buildHashtagInput(String hashtag, Integer maxItems) {
        Map<String, Object> input = new HashMap<>();

        String hashtagUrl = buildHashtagUrl(hashtag);
        input.put("startUrls", List.of(hashtagUrl));

        int items = maxItems != null ? Math.max(maxItems, MIN_ITEMS) : MIN_ITEMS;
        input.put("maxItems", items);

        return input;
    }

    /**
     * Build input for scraping posts from a location
     */
    public Map<String, Object> buildLocationInput(String locationId, Integer maxItems) {
        Map<String, Object> input = new HashMap<>();

        String locationUrl = buildLocationUrl(locationId);
        input.put("startUrls", List.of(locationUrl));

        int items = maxItems != null ? Math.max(maxItems, MIN_ITEMS) : MIN_ITEMS;
        input.put("maxItems", items);

        return input;
    }

    /**
     * Build input for scraping posts from an audio
     */
    public Map<String, Object> buildAudioInput(String audioId, Integer maxItems) {
        Map<String, Object> input = new HashMap<>();

        String audioUrl = buildAudioUrl(audioId);
        input.put("startUrls", List.of(audioUrl));

        int items = maxItems != null ? Math.max(maxItems, MIN_ITEMS) : MIN_ITEMS;
        input.put("maxItems", items);

        return input;
    }

    /**
     * Build input for scraping reels from a profile
     */
    public Map<String, Object> buildReelsInput(String userId, Integer maxItems) {
        Map<String, Object> input = new HashMap<>();

        String reelsUrl = buildReelsUrl(userId);
        input.put("startUrls", List.of(reelsUrl));

        int items = maxItems != null ? Math.max(maxItems, MIN_ITEMS) : MIN_ITEMS;
        input.put("maxItems", items);

        return input;
    }

    /**
     * Build input for scraping posts where user is tagged
     */
    public Map<String, Object> buildTaggedInput(String userId, Integer maxItems) {
        Map<String, Object> input = new HashMap<>();

        String taggedUrl = buildTaggedUrl(userId);
        input.put("startUrls", List.of(taggedUrl));

        int items = maxItems != null ? Math.max(maxItems, MIN_ITEMS) : MIN_ITEMS;
        input.put("maxItems", items);

        return input;
    }

    /**
     * Build input with multiple URLs and optional date filter
     */
    public Map<String, Object> buildMultiUrlInput(List<String> urls, Integer maxItems, String untilDate) {
        Map<String, Object> input = new HashMap<>();

        input.put("startUrls", urls);

        int items = maxItems != null ? Math.max(maxItems, MIN_ITEMS) : MIN_ITEMS;
        input.put("maxItems", items);

        // Optional date filter (format: "2023-12-31")
        if (untilDate != null && !untilDate.isEmpty()) {
            input.put("until", untilDate);
        }

        return input;
    }

    /**
     * Build profile URL from username
     * Handles both with and without @ symbol
     */
    private String buildProfileUrl(String userId) {
        String username = removeAtSymbol(userId);
        return String.format("https://www.instagram.com/%s/", username);
    }

    /**
     * Build hashtag URL
     * Example: https://www.instagram.com/explore/tags/travel
     */
    private String buildHashtagUrl(String hashtag) {
        String tag = removeHashSymbol(hashtag);
        return String.format("https://www.instagram.com/explore/tags/%s", tag);
    }

    /**
     * Build location URL
     * Example: https://www.instagram.com/explore/locations/213131048/berlin-germany/
     */
    private String buildLocationUrl(String locationId) {
        // If locationId contains full URL, return as is
        if (locationId.startsWith("http")) {
            return locationId;
        }
        // Otherwise, build URL (location slug is optional)
        return String.format("https://www.instagram.com/explore/locations/%s/", locationId);
    }

    /**
     * Build audio URL
     * Example: https://www.instagram.com/reels/audio/271328201351336/
     */
    private String buildAudioUrl(String audioId) {
        return String.format("https://www.instagram.com/reels/audio/%s/", audioId);
    }

    /**
     * Build reels URL for a profile
     * Example: https://www.instagram.com/taylorswift/reels
     */
    private String buildReelsUrl(String userId) {
        String username = removeAtSymbol(userId);
        return String.format("https://www.instagram.com/%s/reels", username);
    }

    /**
     * Build tagged URL for a profile
     * Example: https://www.instagram.com/taylorswift/tagged
     */
    private String buildTaggedUrl(String userId) {
        String username = removeAtSymbol(userId);
        return String.format("https://www.instagram.com/%s/tagged", username);
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

    /**
     * Remove # symbol from hashtag if present
     */
    private String removeHashSymbol(String hashtag) {
        if (hashtag == null) {
            return null;
        }
        return hashtag.startsWith("#") ? hashtag.substring(1) : hashtag;
    }
}
