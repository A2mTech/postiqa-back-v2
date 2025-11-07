package fr.postiqa.core.infrastructure.client.analysis.registry;

import fr.postiqa.core.domain.enums.AnalysisType;
import fr.postiqa.core.infrastructure.client.analysis.strategy.AnalysisStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for analysis strategy implementations
 * Provides centralized access to analysis strategies by type
 */
@Component
public class AnalysisStrategyRegistry {

    private static final Logger log = LoggerFactory.getLogger(AnalysisStrategyRegistry.class);

    private final Map<AnalysisType, AnalysisStrategy<?>> strategies = new ConcurrentHashMap<>();

    /**
     * Register a strategy implementation
     *
     * @param strategy Strategy implementation to register
     */
    public void registerStrategy(AnalysisStrategy<?> strategy) {
        AnalysisType analysisType = strategy.getAnalysisType();
        strategies.put(analysisType, strategy);
        log.info("Registered analysis strategy: {}", analysisType.getDisplayName());
    }

    /**
     * Get a strategy by analysis type
     *
     * @param analysisType Type of analysis
     * @return AnalysisStrategy implementation
     * @throws IllegalArgumentException if strategy not found
     */
    @SuppressWarnings("unchecked")
    public <T> AnalysisStrategy<T> getStrategy(AnalysisType analysisType) {
        AnalysisStrategy<?> strategy = strategies.get(analysisType);
        if (strategy == null) {
            throw new IllegalArgumentException(
                "Strategy not available: " + analysisType.getDisplayName() +
                ". Available strategies: " + strategies.keySet()
            );
        }
        return (AnalysisStrategy<T>) strategy;
    }

    /**
     * Check if a strategy is registered
     *
     * @param analysisType Type of analysis to check
     * @return true if strategy is available
     */
    public boolean hasStrategy(AnalysisType analysisType) {
        return strategies.containsKey(analysisType);
    }

    /**
     * Get all registered strategies
     *
     * @return Map of analysis types to implementations
     */
    public Map<AnalysisType, AnalysisStrategy<?>> getAllStrategies() {
        return Map.copyOf(strategies);
    }

    /**
     * Get strategy that requires vision capabilities
     *
     * @return AnalysisStrategy that requires vision
     */
    public AnalysisStrategy<?> getVisionStrategy() {
        return strategies.values().stream()
            .filter(AnalysisStrategy::requiresVision)
            .findFirst()
            .orElse(null);
    }

    /**
     * Get count of registered strategies
     *
     * @return Number of registered strategies
     */
    public int getStrategyCount() {
        return strategies.size();
    }

    /**
     * Clear all registered strategies (primarily for testing)
     */
    public void clear() {
        log.warn("Clearing all registered strategies");
        strategies.clear();
    }
}
