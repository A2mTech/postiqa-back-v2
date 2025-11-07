package fr.postiqa.core.infrastructure.client.actor.impl;

import fr.postiqa.core.infrastructure.client.actor.ActorInputBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Input builder for LinkedIn Apify actors
 * <p>
 * Builds input in the format expected by LinkedIn scraping actors.
 * Example: apify/linkedin-profile-scraper
 */
public class LinkedInActorInputBuilder implements ActorInputBuilder {

    @Override
    public Map<String, Object> buildPostsInput(String userId, Integer maxItems) {
        Map<String, Object> input = new HashMap<>();

        // LinkedIn actor expects profile URLs
        input.put("startUrls", List.of(
            Map.of("url", "https://www.linkedin.com/in/" + userId)
        ));

        input.put("maxPosts", maxItems != null ? maxItems : 50);
        input.put("scrapeJobPosts", false);
        input.put("scrapeActivities", true);
        input.put("scrapeAbout", false);

        return input;
    }

    @Override
    public Map<String, Object> buildProfileInput(String userId) {
        Map<String, Object> input = new HashMap<>();

        input.put("startUrls", List.of(
            Map.of("url", "https://www.linkedin.com/in/" + userId)
        ));

        input.put("scrapeJobPosts", false);
        input.put("scrapeActivities", false);
        input.put("scrapeAbout", true);
        input.put("scrapeFollowers", true);

        return input;
    }

    @Override
    public Map<String, Object> buildCustomInput(String userId, Map<String, Object> additionalParams) {
        Map<String, Object> input = buildPostsInput(userId, null);
        input.putAll(additionalParams);
        return input;
    }
}
