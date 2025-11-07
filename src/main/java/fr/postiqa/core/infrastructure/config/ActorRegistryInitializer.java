package fr.postiqa.core.infrastructure.config;

import fr.postiqa.core.domain.enums.SocialPlatform;
import fr.postiqa.core.infrastructure.client.actor.ActorConfig;
import fr.postiqa.core.infrastructure.client.actor.ActorRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Initializes the ActorRegistry with configured actors at application startup
 * <p>
 * This component registers all available actors from ApifyProperties
 * and sets up their input builders and output parsers.
 */
@Component
public class ActorRegistryInitializer {

    private static final Logger log = LoggerFactory.getLogger(ActorRegistryInitializer.class);

    private final ActorRegistry actorRegistry;
    private final ApifyProperties apifyProperties;

    public ActorRegistryInitializer(ActorRegistry actorRegistry, ApifyProperties apifyProperties) {
        this.actorRegistry = actorRegistry;
        this.apifyProperties = apifyProperties;
    }

    @PostConstruct
    public void initializeRegistry() {
        log.info("Initializing Actor Registry...");

        // Register platform-specific actors
        registerLinkedInActors();
        registerTikTokActors();
        registerInstagramActors();

        // Register other platforms with generic builders
        registerGenericActors();

        log.info("Actor Registry initialized with {} actors", actorRegistry.getAllActors().size());
    }

    private void registerLinkedInActors() {
        String actorId = apifyProperties.getActorId(SocialPlatform.LINKEDIN);
        if (actorId != null && !actorId.isEmpty()) {
            // LinkedIn Posts Actor
            actorRegistry.registerActor(ActorConfig.builder()
                .actorId(actorId)
                .platform(SocialPlatform.LINKEDIN)
                .type(ActorConfig.ActorType.SOCIAL_POSTS)
                .supportsSync(true) // LinkedIn scraping usually takes more than 5 minutes
                .defaultTimeout(Duration.ofMinutes(10))
                .inputBuilderClass("fr.postiqa.core.infrastructure.client.actor.impl.LinkedInActorInputBuilder")
                .outputParserClass("fr.postiqa.core.infrastructure.client.actor.impl.LinkedInActorOutputParser")
                .build()
            );

            // LinkedIn Profile Actor
            actorRegistry.registerActor(ActorConfig.builder()
                .actorId(actorId)
                .platform(SocialPlatform.LINKEDIN)
                .type(ActorConfig.ActorType.SOCIAL_PROFILE)
                .supportsSync(true) // Profile scraping is usually faster
                .defaultTimeout(Duration.ofMinutes(5))
                .inputBuilderClass("fr.postiqa.core.infrastructure.client.actor.impl.LinkedInActorInputBuilder")
                .outputParserClass("fr.postiqa.core.infrastructure.client.actor.impl.LinkedInActorOutputParser")
                .build()
            );
        }
    }

    private void registerTikTokActors() {
        String actorId = apifyProperties.getActorId(SocialPlatform.TIKTOK);
        if (actorId != null && !actorId.isEmpty()) {
            // TikTok Posts Actor
            actorRegistry.registerActor(ActorConfig.builder()
                .actorId(actorId)
                .platform(SocialPlatform.TIKTOK)
                .type(ActorConfig.ActorType.SOCIAL_POSTS)
                .supportsSync(true) // ScrapTik is fast, supports sync
                .defaultTimeout(Duration.ofMinutes(6)) // 360s max
                .inputBuilderClass("fr.postiqa.core.infrastructure.client.actor.impl.TikTokActorInputBuilder")
                .outputParserClass("fr.postiqa.core.infrastructure.client.actor.impl.TikTokActorOutputParser")
                .build()
            );

            // TikTok Profile Actor
            actorRegistry.registerActor(ActorConfig.builder()
                .actorId(actorId)
                .platform(SocialPlatform.TIKTOK)
                .type(ActorConfig.ActorType.SOCIAL_PROFILE)
                .supportsSync(true) // Profile scraping is fast
                .defaultTimeout(Duration.ofMinutes(5))
                .inputBuilderClass("fr.postiqa.core.infrastructure.client.actor.impl.TikTokActorInputBuilder")
                .outputParserClass("fr.postiqa.core.infrastructure.client.actor.impl.TikTokActorOutputParser")
                .build()
            );
        }
    }

    private void registerInstagramActors() {
        String actorId = apifyProperties.getActorId(SocialPlatform.INSTAGRAM);
        if (actorId != null && !actorId.isEmpty()) {
            // Instagram Posts Actor
            actorRegistry.registerActor(ActorConfig.builder()
                .actorId(actorId)
                .platform(SocialPlatform.INSTAGRAM)
                .type(ActorConfig.ActorType.SOCIAL_POSTS)
                .supportsSync(true) // Very fast: 100-200 posts/sec
                .defaultTimeout(Duration.ofMinutes(5))
                .inputBuilderClass("fr.postiqa.core.infrastructure.client.actor.impl.InstagramActorInputBuilder")
                .outputParserClass("fr.postiqa.core.infrastructure.client.actor.impl.InstagramActorOutputParser")
                .build()
            );

            // Instagram Profile Actor
            actorRegistry.registerActor(ActorConfig.builder()
                .actorId(actorId)
                .platform(SocialPlatform.INSTAGRAM)
                .type(ActorConfig.ActorType.SOCIAL_PROFILE)
                .supportsSync(true) // Profile extraction from posts
                .defaultTimeout(Duration.ofMinutes(3))
                .inputBuilderClass("fr.postiqa.core.infrastructure.client.actor.impl.InstagramActorInputBuilder")
                .outputParserClass("fr.postiqa.core.infrastructure.client.actor.impl.InstagramActorOutputParser")
                .build()
            );
        }
    }

    private void registerGenericActors() {
        registerGenericActorForPlatform(SocialPlatform.TWITTER);
        registerGenericActorForPlatform(SocialPlatform.YOUTUBE);
    }

    private void registerGenericActorForPlatform(SocialPlatform platform) {
        String actorId = apifyProperties.getActorId(platform);
        if (actorId != null && !actorId.isEmpty()) {
            // Posts Actor
            actorRegistry.registerActor(ActorConfig.builder()
                .actorId(actorId)
                .platform(platform)
                .type(ActorConfig.ActorType.SOCIAL_POSTS)
                .supportsSync(false)
                .defaultTimeout(Duration.ofMinutes(10))
                .inputBuilderClass("fr.postiqa.core.infrastructure.client.actor.impl.GenericActorInputBuilder")
                .outputParserClass("fr.postiqa.core.infrastructure.client.actor.impl.GenericActorOutputParser")
                .build()
            );

            // Profile Actor
            actorRegistry.registerActor(ActorConfig.builder()
                .actorId(actorId)
                .platform(platform)
                .type(ActorConfig.ActorType.SOCIAL_PROFILE)
                .supportsSync(true)
                .defaultTimeout(Duration.ofMinutes(5))
                .inputBuilderClass("fr.postiqa.core.infrastructure.client.actor.impl.GenericActorInputBuilder")
                .outputParserClass("fr.postiqa.core.infrastructure.client.actor.impl.GenericActorOutputParser")
                .build()
            );
        }
    }
}
