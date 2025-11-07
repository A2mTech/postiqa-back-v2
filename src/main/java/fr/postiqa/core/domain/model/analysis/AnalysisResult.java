package fr.postiqa.core.domain.model.analysis;

import fr.postiqa.core.domain.enums.AIProvider;
import fr.postiqa.core.domain.enums.AnalysisGranularity;
import fr.postiqa.core.domain.enums.AnalysisType;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

/**
 * Generic wrapper for analysis results with metadata
 *
 * @param <T> Type of analysis result (WritingStyleAnalysis, ContentThemesAnalysis, etc.)
 */
public record AnalysisResult<T>(
    String analysisId,
    AnalysisType type,
    AnalysisGranularity granularity,
    AIProvider provider,
    T result,
    String errorMessage,
    LocalDateTime analyzedAt,
    Duration processingTime,
    Map<String, Object> metadata
) {
    public AnalysisResult {
        // Ensure metadata is never null
        metadata = metadata != null ? Map.copyOf(metadata) : Collections.emptyMap();
    }

    public boolean isSuccess() {
        return result != null && errorMessage == null;
    }

    public boolean isFailure() {
        return result == null && errorMessage != null;
    }

    /**
     * Get token usage from metadata if available
     */
    public Integer getTokenUsage() {
        Object tokens = metadata.get("tokenUsage");
        return tokens instanceof Integer ? (Integer) tokens : null;
    }

    /**
     * Get model name used for this analysis from metadata
     */
    public String getModelName() {
        Object model = metadata.get("modelName");
        return model instanceof String ? (String) model : provider.getDefaultModel();
    }

    public static <T> AnalysisResult<T> success(
        String analysisId,
        AnalysisType type,
        AnalysisGranularity granularity,
        AIProvider provider,
        T result,
        LocalDateTime analyzedAt,
        Duration processingTime,
        Map<String, Object> metadata
    ) {
        return new AnalysisResult<>(
            analysisId,
            type,
            granularity,
            provider,
            result,
            null,
            analyzedAt,
            processingTime,
            metadata
        );
    }

    public static <T> AnalysisResult<T> failure(
        String analysisId,
        AnalysisType type,
        AnalysisGranularity granularity,
        AIProvider provider,
        String errorMessage,
        LocalDateTime analyzedAt,
        Duration processingTime
    ) {
        return new AnalysisResult<>(
            analysisId,
            type,
            granularity,
            provider,
            null,
            errorMessage,
            analyzedAt,
            processingTime,
            Collections.emptyMap()
        );
    }
}
