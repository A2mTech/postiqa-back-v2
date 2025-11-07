package fr.postiqa.core.infrastructure.client.actor;

import fr.postiqa.core.domain.enums.SocialPlatform;
import fr.postiqa.core.infrastructure.exception.InvalidPlatformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Central registry for Apify actors
 * <p>
 * Manages all actor configurations and provides lookup by platform and type.
 * This allows easy swapping of actors without code changes.
 */
@Component
public class ActorRegistry {

    private static final Logger log = LoggerFactory.getLogger(ActorRegistry.class);

    private final Map<String, ActorConfig> actors = new HashMap<>();
    private final Map<Class<?>, Object> builderInstances = new HashMap<>();
    private final Map<Class<?>, Object> parserInstances = new HashMap<>();

    /**
     * Register an actor configuration
     */
    public void registerActor(ActorConfig config) {
        String key = buildKey(config.getPlatform(), config.getType());
        actors.put(key, config);
        log.info("Registered actor: {} for {} ({})", config.getActorId(), config.getPlatform(), config.getType());
    }

    /**
     * Get actor configuration for a platform and type
     */
    public Optional<ActorConfig> getActor(SocialPlatform platform, ActorConfig.ActorType type) {
        String key = buildKey(platform, type);
        return Optional.ofNullable(actors.get(key));
    }

    /**
     * Get actor configuration or throw exception
     */
    public ActorConfig getActorOrThrow(SocialPlatform platform, ActorConfig.ActorType type) {
        return getActor(platform, type)
            .orElseThrow(() -> new InvalidPlatformException(
                platform,
                "No actor configured for type: " + type
            ));
    }

    /**
     * Get input builder for an actor
     */
    @SuppressWarnings("unchecked")
    public <T extends ActorInputBuilder> T getInputBuilder(ActorConfig config) {
        String builderClass = config.getInputBuilderClass();
        if (builderClass == null) {
            throw new IllegalStateException("No input builder configured for actor: " + config.getActorId());
        }

        try {
            Class<?> clazz = Class.forName(builderClass);
            return (T) builderInstances.computeIfAbsent(clazz, k -> {
                try {
                    return k.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to instantiate input builder: " + builderClass, e);
                }
            });
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Input builder class not found: " + builderClass, e);
        }
    }

    /**
     * Get output parser for an actor
     */
    @SuppressWarnings("unchecked")
    public <T extends ActorOutputParser> T getOutputParser(ActorConfig config) {
        String parserClass = config.getOutputParserClass();
        if (parserClass == null) {
            throw new IllegalStateException("No output parser configured for actor: " + config.getActorId());
        }

        try {
            Class<?> clazz = Class.forName(parserClass);
            return (T) parserInstances.computeIfAbsent(clazz, k -> {
                try {
                    return k.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to instantiate output parser: " + parserClass, e);
                }
            });
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Output parser class not found: " + parserClass, e);
        }
    }

    /**
     * Check if an actor is registered for a platform and type
     */
    public boolean hasActor(SocialPlatform platform, ActorConfig.ActorType type) {
        return getActor(platform, type).isPresent();
    }

    /**
     * Get all registered actors
     */
    public Map<String, ActorConfig> getAllActors() {
        return new HashMap<>(actors);
    }

    private String buildKey(SocialPlatform platform, ActorConfig.ActorType type) {
        return platform + ":" + type;
    }
}
