package fr.postiqa.features.postmanagement.domain.vo;

import fr.postiqa.shared.enums.SocialPlatform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Value object representing post content.
 * Automatically extracts hashtags and mentions from text.
 * Validates content length based on platform constraints.
 */
public record Content(
    String text,
    List<String> hashtags,
    List<String> mentions
) {
    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#\\w+");
    private static final Pattern MENTION_PATTERN = Pattern.compile("@\\w+");

    // Platform-specific max lengths
    private static final int LINKEDIN_MAX_LENGTH = 3000;
    private static final int TWITTER_MAX_LENGTH = 280;
    private static final int INSTAGRAM_MAX_LENGTH = 2200;
    private static final int TIKTOK_MAX_LENGTH = 2200;
    private static final int YOUTUBE_MAX_LENGTH = 5000;

    public Content {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Content text cannot be null or blank");
        }
        if (text.length() > YOUTUBE_MAX_LENGTH) {
            throw new IllegalArgumentException("Content text too long: " + text.length() + " characters (max: " + YOUTUBE_MAX_LENGTH + ")");
        }
        hashtags = hashtags == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(hashtags));
        mentions = mentions == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(mentions));
    }

    /**
     * Create content from text, automatically extracting hashtags and mentions
     */
    public static Content of(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Content text cannot be null or blank");
        }

        List<String> extractedHashtags = extractHashtags(text);
        List<String> extractedMentions = extractMentions(text);

        return new Content(text.trim(), extractedHashtags, extractedMentions);
    }

    /**
     * Create content with pre-extracted hashtags and mentions
     */
    public static Content create(String text, List<String> hashtags, List<String> mentions) {
        return new Content(text.trim(), hashtags, mentions);
    }

    /**
     * Check if content is valid for a specific platform
     */
    public boolean isValidForPlatform(SocialPlatform platform) {
        int maxLength = getMaxLengthForPlatform(platform);
        return text.length() <= maxLength;
    }

    /**
     * Get maximum length for a platform
     */
    public static int getMaxLengthForPlatform(SocialPlatform platform) {
        return switch (platform) {
            case LINKEDIN -> LINKEDIN_MAX_LENGTH;
            case TWITTER -> TWITTER_MAX_LENGTH;
            case INSTAGRAM -> INSTAGRAM_MAX_LENGTH;
            case TIKTOK -> TIKTOK_MAX_LENGTH;
            case YOUTUBE -> YOUTUBE_MAX_LENGTH;
        };
    }

    /**
     * Get remaining characters for a platform
     */
    public int getRemainingCharsForPlatform(SocialPlatform platform) {
        return getMaxLengthForPlatform(platform) - text.length();
    }

    /**
     * Extract hashtags from text
     */
    private static List<String> extractHashtags(String text) {
        List<String> hashtags = new ArrayList<>();
        Matcher matcher = HASHTAG_PATTERN.matcher(text);
        while (matcher.find()) {
            hashtags.add(matcher.group().substring(1)); // Remove # prefix
        }
        return Collections.unmodifiableList(hashtags);
    }

    /**
     * Extract mentions from text
     */
    private static List<String> extractMentions(String text) {
        List<String> mentions = new ArrayList<>();
        Matcher matcher = MENTION_PATTERN.matcher(text);
        while (matcher.find()) {
            mentions.add(matcher.group().substring(1)); // Remove @ prefix
        }
        return Collections.unmodifiableList(mentions);
    }

    /**
     * Get character count
     */
    public int length() {
        return text.length();
    }

    /**
     * Check if content contains hashtags
     */
    public boolean hasHashtags() {
        return !hashtags.isEmpty();
    }

    /**
     * Check if content contains mentions
     */
    public boolean hasMentions() {
        return !mentions.isEmpty();
    }
}
