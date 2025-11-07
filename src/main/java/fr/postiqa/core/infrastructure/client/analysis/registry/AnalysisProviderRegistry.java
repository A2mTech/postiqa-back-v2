package fr.postiqa.core.infrastructure.client.analysis.registry;

import fr.postiqa.core.domain.enums.AIProvider;
import fr.postiqa.core.infrastructure.client.analysis.provider.AnalysisProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for AI provider implementations
 * Provides centralized access to configured providers (OpenAI, Gemini, etc.)
 */
@Component
public class AnalysisProviderRegistry {

    private static final Logger log = LoggerFactory.getLogger(AnalysisProviderRegistry.class);

    private final Map<AIProvider, AnalysisProvider> providers = new ConcurrentHashMap<>();

    /**
     * Register a provider implementation
     *
     * @param provider Provider implementation to register
     */
    public void registerProvider(AnalysisProvider provider) {
        AIProvider providerType = provider.getProviderType();
        providers.put(providerType, provider);
        log.info("Registered analysis provider: {} (model: {})",
            providerType.getDisplayName(),
            provider.getModelName());
    }

    /**
     * Get a provider by type
     *
     * @param providerType Provider type to retrieve
     * @return AnalysisProvider implementation
     * @throws IllegalArgumentException if provider not found
     */
    public AnalysisProvider getProvider(AIProvider providerType) {
        AnalysisProvider provider = providers.get(providerType);
        if (provider == null) {
            throw new IllegalArgumentException(
                "Provider not available: " + providerType.getDisplayName() +
                ". Available providers: " + providers.keySet()
            );
        }
        return provider;
    }

    /**
     * Check if a provider is registered
     *
     * @param providerType Provider type to check
     * @return true if provider is available
     */
    public boolean hasProvider(AIProvider providerType) {
        return providers.containsKey(providerType);
    }

    /**
     * Get all registered providers
     *
     * @return Map of provider types to implementations
     */
    public Map<AIProvider, AnalysisProvider> getAllProviders() {
        return Map.copyOf(providers);
    }

    /**
     * Get the first available provider that supports vision
     *
     * @return AnalysisProvider that supports vision
     * @throws IllegalStateException if no vision-capable provider is available
     */
    public AnalysisProvider getVisionCapableProvider() {
        return providers.values().stream()
            .filter(AnalysisProvider::supportsVision)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No vision-capable provider available"));
    }

    /**
     * Get the first available provider that supports multimodal analysis
     *
     * @return AnalysisProvider that supports multimodal
     * @throws IllegalStateException if no multimodal-capable provider is available
     */
    public AnalysisProvider getMultimodalCapableProvider() {
        return providers.values().stream()
            .filter(AnalysisProvider::supportsMultimodal)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No multimodal-capable provider available"));
    }

    /**
     * Get count of registered providers
     *
     * @return Number of registered providers
     */
    public int getProviderCount() {
        return providers.size();
    }

    /**
     * Clear all registered providers (primarily for testing)
     */
    public void clear() {
        log.warn("Clearing all registered providers");
        providers.clear();
    }
}
