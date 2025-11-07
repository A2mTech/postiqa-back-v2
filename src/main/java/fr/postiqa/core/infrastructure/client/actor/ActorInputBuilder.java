package fr.postiqa.core.infrastructure.client.actor;

import java.util.Map;

/**
 * Interface for building actor-specific input payloads
 * <p>
 * Each actor has its own input format. Implementations of this interface
 * know how to construct the correct input for a specific actor.
 */
public interface ActorInputBuilder {

    /**
     * Build input for scraping posts
     *
     * @param userId   the user ID on the platform
     * @param maxItems maximum number of items to scrape
     * @return input map for the actor
     */
    Map<String, Object> buildPostsInput(String userId, Integer maxItems);

    /**
     * Build input for scraping a profile
     *
     * @param userId the user ID on the platform
     * @return input map for the actor
     */
    Map<String, Object> buildProfileInput(String userId);

    /**
     * Build custom input with additional parameters
     *
     * @param userId           the user ID on the platform
     * @param additionalParams additional parameters specific to this actor
     * @return input map for the actor
     */
    Map<String, Object> buildCustomInput(String userId, Map<String, Object> additionalParams);
}
