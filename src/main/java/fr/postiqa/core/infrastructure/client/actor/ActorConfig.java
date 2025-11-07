package fr.postiqa.core.infrastructure.client.actor;

import fr.postiqa.core.domain.enums.SocialPlatform;

import java.time.Duration;

/**
 * Configuration for an Apify Actor
 * <p>
 * Defines how to call an actor, what input format to use,
 * and how to parse its output.
 */
public class ActorConfig {

    private final String actorId;
    private final SocialPlatform platform;
    private final ActorType type;
    private final Duration defaultTimeout;
    private final boolean supportsSync;
    private final String inputBuilderClass;
    private final String outputParserClass;

    private ActorConfig(Builder builder) {
        this.actorId = builder.actorId;
        this.platform = builder.platform;
        this.type = builder.type;
        this.defaultTimeout = builder.defaultTimeout;
        this.supportsSync = builder.supportsSync;
        this.inputBuilderClass = builder.inputBuilderClass;
        this.outputParserClass = builder.outputParserClass;
    }

    public String getActorId() {
        return actorId;
    }

    public SocialPlatform getPlatform() {
        return platform;
    }

    public ActorType getType() {
        return type;
    }

    public Duration getDefaultTimeout() {
        return defaultTimeout;
    }

    public boolean isSupportsSync() {
        return supportsSync;
    }

    public String getInputBuilderClass() {
        return inputBuilderClass;
    }

    public String getOutputParserClass() {
        return outputParserClass;
    }

    public static Builder builder() {
        return new Builder();
    }

    public enum ActorType {
        SOCIAL_POSTS,
        SOCIAL_PROFILE,
        WEB_SCRAPER
    }

    public static class Builder {
        private String actorId;
        private SocialPlatform platform;
        private ActorType type;
        private Duration defaultTimeout = Duration.ofMinutes(5);
        private boolean supportsSync = false;
        private String inputBuilderClass;
        private String outputParserClass;

        public Builder actorId(String actorId) {
            this.actorId = actorId;
            return this;
        }

        public Builder platform(SocialPlatform platform) {
            this.platform = platform;
            return this;
        }

        public Builder type(ActorType type) {
            this.type = type;
            return this;
        }

        public Builder defaultTimeout(Duration defaultTimeout) {
            this.defaultTimeout = defaultTimeout;
            return this;
        }

        public Builder supportsSync(boolean supportsSync) {
            this.supportsSync = supportsSync;
            return this;
        }

        public Builder inputBuilderClass(String inputBuilderClass) {
            this.inputBuilderClass = inputBuilderClass;
            return this;
        }

        public Builder outputParserClass(String outputParserClass) {
            this.outputParserClass = outputParserClass;
            return this;
        }

        public ActorConfig build() {
            if (actorId == null || actorId.isBlank()) {
                throw new IllegalArgumentException("Actor ID is required");
            }
            if (type == null) {
                throw new IllegalArgumentException("Actor type is required");
            }
            return new ActorConfig(this);
        }
    }

    @Override
    public String toString() {
        return "ActorConfig{" +
            "actorId='" + actorId + '\'' +
            ", platform=" + platform +
            ", type=" + type +
            ", supportsSync=" + supportsSync +
            '}';
    }
}
