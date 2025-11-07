package fr.postiqa.core.infrastructure.client.analysis.prompt;

import fr.postiqa.core.domain.enums.AnalysisGranularity;
import fr.postiqa.core.domain.enums.SocialPlatform;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Context data for prompt interpolation
 */
public record PromptContext(
    AnalysisGranularity granularity,
    SocialPlatform platform,
    Integer itemCount,
    Map<String, Object> customVariables
) {
    public PromptContext {
        customVariables = customVariables != null ? Map.copyOf(customVariables) : Collections.emptyMap();
    }

    public Object getVariable(String key) {
        return customVariables.get(key);
    }

    public String getVariableAsString(String key) {
        Object value = customVariables.get(key);
        return value != null ? value.toString() : null;
    }

    public boolean hasVariable(String key) {
        return customVariables.containsKey(key);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static PromptContext of(AnalysisGranularity granularity) {
        return new PromptContext(granularity, null, null, Collections.emptyMap());
    }

    public static PromptContext of(AnalysisGranularity granularity, SocialPlatform platform) {
        return new PromptContext(granularity, platform, null, Collections.emptyMap());
    }

    public static class Builder {
        private AnalysisGranularity granularity;
        private SocialPlatform platform;
        private Integer itemCount;
        private final Map<String, Object> customVariables = new HashMap<>();

        public Builder granularity(AnalysisGranularity granularity) {
            this.granularity = granularity;
            return this;
        }

        public Builder platform(SocialPlatform platform) {
            this.platform = platform;
            return this;
        }

        public Builder itemCount(Integer itemCount) {
            this.itemCount = itemCount;
            return this;
        }

        public Builder variable(String key, Object value) {
            this.customVariables.put(key, value);
            return this;
        }

        public Builder variables(Map<String, Object> variables) {
            this.customVariables.putAll(variables);
            return this;
        }

        public PromptContext build() {
            return new PromptContext(granularity, platform, itemCount, customVariables);
        }
    }
}
