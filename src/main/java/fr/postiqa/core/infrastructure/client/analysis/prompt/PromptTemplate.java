package fr.postiqa.core.infrastructure.client.analysis.prompt;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Immutable prompt template with variable interpolation support
 * Variables are defined using {{variableName}} syntax
 */
public record PromptTemplate(String template) {
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)}}");

    /**
     * Interpolate variables in the template
     *
     * @param variables Map of variable name to value
     * @return Interpolated prompt string
     */
    public String interpolate(Map<String, Object> variables) {
        if (template == null || template.isEmpty()) {
            return "";
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String variableName = matcher.group(1).trim();
            Object value = variables.get(variableName);
            String replacement = value != null ? value.toString() : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Interpolate variables from PromptContext
     *
     * @param context PromptContext containing variables
     * @return Interpolated prompt string
     */
    public String interpolate(PromptContext context) {
        Map<String, Object> variables = context.customVariables();

        // Add standard context fields to variables
        if (context.granularity() != null) {
            variables = new java.util.HashMap<>(variables);
            variables.put("granularity", context.granularity().getDisplayName());
        }
        if (context.platform() != null) {
            if (variables == context.customVariables()) {
                variables = new java.util.HashMap<>(variables);
            }
            variables.put("platform", context.platform().getDisplayName());
        }
        if (context.itemCount() != null) {
            if (variables == context.customVariables()) {
                variables = new java.util.HashMap<>(variables);
            }
            variables.put("itemCount", context.itemCount());
        }

        return interpolate(variables);
    }

    /**
     * Check if template contains a specific variable
     *
     * @param variableName Variable name to check
     * @return true if template contains the variable
     */
    public boolean hasVariable(String variableName) {
        return template != null && template.contains("{{" + variableName + "}}");
    }

    public static PromptTemplate of(String template) {
        return new PromptTemplate(template);
    }

    public static PromptTemplate empty() {
        return new PromptTemplate("");
    }
}
