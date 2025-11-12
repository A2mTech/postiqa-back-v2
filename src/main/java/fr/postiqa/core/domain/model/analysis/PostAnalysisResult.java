package fr.postiqa.core.domain.model.analysis;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Granular analysis result for a single social media post.
 * Supports all post types: text, image, carousel, video, thread.
 * Maps to Phase 2C in the ultra-deep analysis workflow.
 */
public record PostAnalysisResult(
    String postId,
    String postUrl,
    String postType,  // text, image, carousel, video, thread
    String textContent,
    Instant publishedAt,
    Map<String, Integer> engagement,  // likes, comments, shares, views
    ContentAnalysis contentAnalysis,
    StructureAnalysis structureAnalysis,
    WritingStyleProfile writingStyle,
    Map<String, Object> formatting,
    Map<String, Object> visualAnalysis,  // For image/carousel/video posts
    Map<String, Object> replicabilityInsights,
    Map<String, Object> rawData
) {
    public PostAnalysisResult {
        engagement = engagement != null ? Map.copyOf(engagement) : Collections.emptyMap();
        formatting = formatting != null ? Map.copyOf(formatting) : Collections.emptyMap();
        visualAnalysis = visualAnalysis != null ? Map.copyOf(visualAnalysis) : Collections.emptyMap();
        replicabilityInsights = replicabilityInsights != null ? Map.copyOf(replicabilityInsights) : Collections.emptyMap();
        rawData = rawData != null ? Map.copyOf(rawData) : Collections.emptyMap();
    }

    public boolean isTextPost() {
        return "text".equals(postType);
    }

    public boolean isVisualPost() {
        return "image".equals(postType) || "carousel".equals(postType) || "video".equals(postType);
    }

    public int getTotalEngagement() {
        return engagement.values().stream().mapToInt(Integer::intValue).sum();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String postId;
        private String postUrl;
        private String postType;
        private String textContent;
        private Instant publishedAt;
        private Map<String, Integer> engagement = Collections.emptyMap();
        private ContentAnalysis contentAnalysis;
        private StructureAnalysis structureAnalysis;
        private WritingStyleProfile writingStyle;
        private Map<String, Object> formatting = Collections.emptyMap();
        private Map<String, Object> visualAnalysis = Collections.emptyMap();
        private Map<String, Object> replicabilityInsights = Collections.emptyMap();
        private Map<String, Object> rawData = Collections.emptyMap();

        public Builder postId(String postId) {
            this.postId = postId;
            return this;
        }

        public Builder postUrl(String postUrl) {
            this.postUrl = postUrl;
            return this;
        }

        public Builder postType(String postType) {
            this.postType = postType;
            return this;
        }

        public Builder textContent(String textContent) {
            this.textContent = textContent;
            return this;
        }

        public Builder publishedAt(Instant publishedAt) {
            this.publishedAt = publishedAt;
            return this;
        }

        public Builder engagement(Map<String, Integer> engagement) {
            this.engagement = engagement;
            return this;
        }

        public Builder contentAnalysis(ContentAnalysis contentAnalysis) {
            this.contentAnalysis = contentAnalysis;
            return this;
        }

        public Builder structureAnalysis(StructureAnalysis structureAnalysis) {
            this.structureAnalysis = structureAnalysis;
            return this;
        }

        public Builder writingStyle(WritingStyleProfile writingStyle) {
            this.writingStyle = writingStyle;
            return this;
        }

        public Builder formatting(Map<String, Object> formatting) {
            this.formatting = formatting;
            return this;
        }

        public Builder visualAnalysis(Map<String, Object> visualAnalysis) {
            this.visualAnalysis = visualAnalysis;
            return this;
        }

        public Builder replicabilityInsights(Map<String, Object> replicabilityInsights) {
            this.replicabilityInsights = replicabilityInsights;
            return this;
        }

        public Builder rawData(Map<String, Object> rawData) {
            this.rawData = rawData;
            return this;
        }

        public PostAnalysisResult build() {
            return new PostAnalysisResult(
                postId,
                postUrl,
                postType,
                textContent,
                publishedAt,
                engagement,
                contentAnalysis,
                structureAnalysis,
                writingStyle,
                formatting,
                visualAnalysis,
                replicabilityInsights,
                rawData
            );
        }
    }
}

/**
 * Content analysis for a post
 */
record ContentAnalysis(
    String mainTopic,
    List<String> subtopics,
    String intent,  // educate, entertain, inspire, promote, engage, storytelling
    int messageClarity  // 1-10
) {
    public ContentAnalysis {
        subtopics = subtopics != null ? List.copyOf(subtopics) : Collections.emptyList();
    }
}

/**
 * Structure analysis for a post
 */
record StructureAnalysis(
    Hook hook,
    Body body,
    CallToAction cta
) {}

/**
 * Hook analysis
 */
record Hook(
    String type,  // question, stat, story, statement, shock, other
    String text,
    int effectiveness  // 1-10
) {}

/**
 * Body analysis
 */
record Body(
    String structureType,  // storytelling, list, argument, tutorial, case_study
    int paragraphCount,
    int flowQuality  // 1-10
) {}

/**
 * CTA analysis
 */
record CallToAction(
    boolean present,
    String type,  // question, link, comment_request, share, dm, none
    String text
) {}

/**
 * Writing style profile for a single post
 */
record WritingStyleProfile(
    List<String> tone,  // casual, professional, provocative, etc.
    String voice,  // personal, expert, storyteller
    String readingLevel,  // simple, intermediate, advanced
    List<String> emotionalAppeal,  // inspiring, humorous, serious
    String sentenceLength  // short, medium, long, mixed
) {
    public WritingStyleProfile {
        tone = tone != null ? List.copyOf(tone) : Collections.emptyList();
        emotionalAppeal = emotionalAppeal != null ? List.copyOf(emotionalAppeal) : Collections.emptyList();
    }
}
