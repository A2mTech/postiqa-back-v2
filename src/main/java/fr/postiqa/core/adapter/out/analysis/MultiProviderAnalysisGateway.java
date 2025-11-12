package fr.postiqa.core.adapter.out.analysis;

import fr.postiqa.core.domain.enums.AIProvider;
import fr.postiqa.core.domain.enums.AnalysisGranularity;
import fr.postiqa.core.domain.enums.AnalysisType;
import fr.postiqa.core.domain.model.SocialPost;
import fr.postiqa.core.domain.model.analysis.*;
import fr.postiqa.core.domain.port.AnalysisPort;
import fr.postiqa.core.infrastructure.client.analysis.registry.AnalysisStrategyRegistry;
import fr.postiqa.core.infrastructure.client.analysis.strategy.AnalysisStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Gateway implementation for multi-provider AI analysis
 * Delegates to appropriate strategies based on analysis type
 */
@Component
public class MultiProviderAnalysisGateway implements AnalysisPort {

    private static final Logger log = LoggerFactory.getLogger(MultiProviderAnalysisGateway.class);

    private final AnalysisStrategyRegistry strategyRegistry;

    public MultiProviderAnalysisGateway(AnalysisStrategyRegistry strategyRegistry) {
        this.strategyRegistry = strategyRegistry;
    }

    @Override
    public AnalysisResult<WritingStyleAnalysis> analyzeWritingStyle(
        List<SocialPost> posts,
        AnalysisGranularity granularity,
        AIProvider provider
    ) {
        log.debug("Analyzing writing style - posts: {}, granularity: {}, provider: {}",
            posts.size(), granularity, provider);

        AnalysisStrategy<WritingStyleAnalysis> strategy =
            strategyRegistry.getStrategy(AnalysisType.WRITING_STYLE);

        return strategy.analyze(posts, granularity, provider);
    }

    @Override
    public AnalysisResult<ContentThemesAnalysis> analyzeContentThemes(
        List<SocialPost> posts,
        AnalysisGranularity granularity,
        AIProvider provider
    ) {
        log.debug("Analyzing content themes - posts: {}, granularity: {}, provider: {}",
            posts.size(), granularity, provider);

        AnalysisStrategy<ContentThemesAnalysis> strategy =
            strategyRegistry.getStrategy(AnalysisType.CONTENT_THEMES);

        return strategy.analyze(posts, granularity, provider);
    }

    @Override
    public AnalysisResult<ImageAnalysisResult> analyzeImages(
        List<SocialPost> posts,
        AnalysisGranularity granularity,
        AIProvider provider
    ) {
        log.debug("Analyzing images - posts: {}, granularity: {}, provider: {}",
            posts.size(), granularity, provider);

        // Validate provider supports vision
        if (!provider.supportsVisionAnalysis()) {
            throw new IllegalArgumentException(
                "Provider " + provider.getDisplayName() + " does not support vision analysis"
            );
        }

        AnalysisStrategy<ImageAnalysisResult> strategy =
            strategyRegistry.getStrategy(AnalysisType.IMAGE_ANALYSIS);

        return strategy.analyze(posts, granularity, provider);
    }

    @Override
    public AnalysisResult<MultimodalAnalysisResult> analyzeMultimodal(
        List<SocialPost> posts,
        AnalysisGranularity granularity,
        AIProvider provider
    ) {
        log.debug("Analyzing multimodal content - posts: {}, granularity: {}, provider: {}",
            posts.size(), granularity, provider);

        // Validate provider supports multimodal
        if (!provider.supportsMultimodal()) {
            throw new IllegalArgumentException(
                "Provider " + provider.getDisplayName() + " does not support multimodal analysis"
            );
        }

        AnalysisStrategy<MultimodalAnalysisResult> strategy =
            strategyRegistry.getStrategy(AnalysisType.MULTIMODAL);

        return strategy.analyze(posts, granularity, provider);
    }

    @Override
    public AnalysisResult<?> analyze(
        List<SocialPost> posts,
        AnalysisType analysisType,
        AnalysisGranularity granularity,
        AIProvider provider
    ) {
        log.debug("Generic analysis - type: {}, posts: {}, granularity: {}, provider: {}",
            analysisType, posts.size(), granularity, provider);

        // Validate provider capabilities for the requested analysis type
        if (!provider.supports(analysisType)) {
            throw new IllegalArgumentException(
                "Provider " + provider.getDisplayName() +
                " does not support " + analysisType.getDisplayName()
            );
        }

        return switch (analysisType) {
            case WRITING_STYLE -> analyzeWritingStyle(posts, granularity, provider);
            case CONTENT_THEMES -> analyzeContentThemes(posts, granularity, provider);
            case IMAGE_ANALYSIS -> analyzeImages(posts, granularity, provider);
            case MULTIMODAL -> analyzeMultimodal(posts, granularity, provider);
            default -> throw new UnsupportedOperationException(
                "Analysis type " + analysisType.getDisplayName() +
                " is not supported by this method. Use specialized analysis workflows instead."
            );
        };
    }
}
