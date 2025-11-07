package fr.postiqa.core.infrastructure.config;

import fr.postiqa.core.infrastructure.client.analysis.provider.AnalysisProvider;
import fr.postiqa.core.infrastructure.client.analysis.registry.AnalysisProviderRegistry;
import fr.postiqa.core.infrastructure.client.analysis.registry.AnalysisStrategyRegistry;
import fr.postiqa.core.infrastructure.client.analysis.strategy.AnalysisStrategy;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Initializes analysis registries with all available providers and strategies at application startup
 */
@Component
public class AnalysisRegistryInitializer {

    private static final Logger log = LoggerFactory.getLogger(AnalysisRegistryInitializer.class);

    private final ApplicationContext applicationContext;
    private final AnalysisProviderRegistry providerRegistry;
    private final AnalysisStrategyRegistry strategyRegistry;

    public AnalysisRegistryInitializer(
        ApplicationContext applicationContext,
        AnalysisProviderRegistry providerRegistry,
        AnalysisStrategyRegistry strategyRegistry
    ) {
        this.applicationContext = applicationContext;
        this.providerRegistry = providerRegistry;
        this.strategyRegistry = strategyRegistry;
    }

    @PostConstruct
    public void initialize() {
        log.info("Initializing analysis registries...");

        registerProviders();
        registerStrategies();

        log.info("Analysis registries initialized successfully - {} providers, {} strategies",
            providerRegistry.getProviderCount(),
            strategyRegistry.getStrategyCount());
    }

    private void registerProviders() {
        log.info("Registering analysis providers...");

        Map<String, AnalysisProvider> providers = applicationContext.getBeansOfType(AnalysisProvider.class);

        if (providers.isEmpty()) {
            log.warn("No analysis providers found in application context");
            return;
        }

        providers.values().forEach(provider -> {
            try {
                providerRegistry.registerProvider(provider);
                log.debug("Registered provider: {} with model: {}",
                    provider.getProviderType().getDisplayName(),
                    provider.getModelName());
            } catch (Exception e) {
                log.error("Failed to register provider {}: {}",
                    provider.getClass().getSimpleName(),
                    e.getMessage());
            }
        });

        log.info("Registered {} analysis provider(s)", providerRegistry.getProviderCount());
    }

    private void registerStrategies() {
        log.info("Registering analysis strategies...");

        Map<String, AnalysisStrategy> strategies = applicationContext.getBeansOfType(AnalysisStrategy.class);

        if (strategies.isEmpty()) {
            log.warn("No analysis strategies found in application context");
            return;
        }

        strategies.values().forEach(strategy -> {
            try {
                strategyRegistry.registerStrategy(strategy);
                log.debug("Registered strategy: {}",
                    strategy.getAnalysisType().getDisplayName());
            } catch (Exception e) {
                log.error("Failed to register strategy {}: {}",
                    strategy.getClass().getSimpleName(),
                    e.getMessage());
            }
        });

        log.info("Registered {} analysis strateg(ies)", strategyRegistry.getStrategyCount());
    }
}
