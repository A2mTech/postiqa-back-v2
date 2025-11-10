package fr.postiqa.core.infrastructure.workflow.state;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.postiqa.core.domain.workflow.model.WorkflowContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Serializes and deserializes WorkflowContext to/from JSON.
 * Used for persisting context in database JSONB columns.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ContextSerializer {

    private final ObjectMapper objectMapper;

    /**
     * Serialize WorkflowContext to JSON string
     *
     * @param context The workflow context
     * @return JSON string representation
     * @throws SerializationException if serialization fails
     */
    public String serialize(WorkflowContext context) {
        try {
            String json = objectMapper.writeValueAsString(context.data());
            log.debug("Serialized context with {} entries", context.size());
            return json;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize workflow context", e);
            throw new SerializationException("Failed to serialize workflow context", e);
        }
    }

    /**
     * Deserialize JSON string to WorkflowContext
     *
     * @param json The JSON string
     * @return The deserialized workflow context
     * @throws SerializationException if deserialization fails
     */
    @SuppressWarnings("unchecked")
    public WorkflowContext deserialize(String json) {
        if (json == null || json.isBlank()) {
            return WorkflowContext.empty();
        }

        try {
            Map<String, Object> data = objectMapper.readValue(json, HashMap.class);
            log.debug("Deserialized context with {} entries", data.size());
            return WorkflowContext.of(data);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize workflow context", e);
            throw new SerializationException("Failed to deserialize workflow context", e);
        }
    }

    /**
     * Deserialize from Map (already deserialized by JPA)
     */
    public WorkflowContext deserialize(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return WorkflowContext.empty();
        }
        return WorkflowContext.of(data);
    }

    /**
     * Serialize to Map (for JPA JSONB)
     */
    public Map<String, Object> serializeToMap(WorkflowContext context) {
        return new HashMap<>(context.data());
    }

    /**
     * Exception thrown when serialization/deserialization fails
     */
    public static class SerializationException extends RuntimeException {
        public SerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
