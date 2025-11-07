package fr.postiqa.core.domain.model;

import fr.postiqa.core.domain.enums.ContentType;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents content scraped from a website
 */
public record WebsiteContent(
    String url,
    ContentType type,
    String title,
    String mainContent,
    String excerpt,
    List<String> imageUrls,
    String author,
    LocalDateTime publishedDate,
    Map<String, String> metadata,
    LocalDateTime scrapedAt
) {
    public WebsiteContent {
        // Ensure collections are never null
        imageUrls = imageUrls != null ? List.copyOf(imageUrls) : Collections.emptyList();
        metadata = metadata != null ? Map.copyOf(metadata) : Collections.emptyMap();
        scrapedAt = scrapedAt != null ? scrapedAt : LocalDateTime.now();
    }

    public boolean hasImages() {
        return !imageUrls.isEmpty();
    }

    public boolean hasExcerpt() {
        return excerpt != null && !excerpt.isBlank();
    }

    public boolean hasAuthor() {
        return author != null && !author.isBlank();
    }

    public int wordCount() {
        if (mainContent == null || mainContent.isBlank()) {
            return 0;
        }
        return mainContent.split("\\s+").length;
    }
}
