package fr.postiqa.core.domain.enums;

/**
 * Enumeration of post types for content analysis.
 * <p>
 * Used to categorize social media posts by their primary content format.
 */
public enum PostType {
    /**
     * Text-only post (no media)
     */
    TEXT,

    /**
     * Single image post
     */
    IMAGE,

    /**
     * Single video post
     */
    VIDEO,

    /**
     * Carousel/album post (multiple images/videos)
     */
    CAROUSEL,

    /**
     * Thread post (multiple connected posts, primarily Twitter)
     */
    THREAD,

    /**
     * Unknown or unclassified post type
     */
    UNKNOWN
}
