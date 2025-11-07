package fr.postiqa.core.infrastructure.client.actor;

import fr.postiqa.core.domain.model.SocialPost;
import fr.postiqa.core.domain.model.SocialProfile;

import java.util.List;
import java.util.Map;

/**
 * Interface for parsing actor-specific output
 * <p>
 * Each actor returns data in its own format. Implementations of this interface
 * know how to parse the output from a specific actor into domain models.
 */
public interface ActorOutputParser {

    /**
     * Parse posts from actor output
     *
     * @param items raw items from actor dataset
     * @return list of parsed social posts
     */
    List<SocialPost> parsePosts(List<Map<String, Object>> items);

    /**
     * Parse a profile from actor output
     *
     * @param item raw item from actor dataset
     * @return parsed social profile
     */
    SocialProfile parseProfile(Map<String, Object> item);
}
