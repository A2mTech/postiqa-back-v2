package fr.postiqa.core.adapter.out;

import fr.postiqa.core.domain.port.AnalysisPort;
import fr.postiqa.core.infrastructure.config.OpenAIProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * OpenAI implementation of AnalysisPort
 * <p>
 * Uses OpenAI API to analyze writing style from social media posts.
 * <p>
 * TODO: Full implementation with Spring AI integration
 */
@Component
public class OpenAIAnalysisGateway implements AnalysisPort {

    private static final Logger log = LoggerFactory.getLogger(OpenAIAnalysisGateway.class);

    private final OpenAIProperties properties;

    public OpenAIAnalysisGateway(OpenAIProperties properties) {
        this.properties = properties;
    }

    @Override
    public WritingProfile analyzeWritingStyle(List<String> posts) {
        log.info("Analyzing writing style from {} posts using OpenAI", posts.size());

        // TODO: Implement OpenAI API call using Spring AI
        // 1. Build prompt with posts
        // 2. Call OpenAI chat completion
        // 3. Parse response into WritingProfile

        // Stub implementation for now
        return new WritingProfile(
            "Professional and engaging",
            "Story-driven with clear structure",
            List.of("Did you know...", "Here's the thing...", "Let me tell you about..."),
            "Minimal, only when emphasizing",
            "Business-focused with occasional metaphors"
        );
    }

    // TODO: Add helper methods for:
    // - buildAnalysisPrompt(List<String> posts)
    // - callOpenAI(String prompt)
    // - parseOpenAIResponse(String response)
}
