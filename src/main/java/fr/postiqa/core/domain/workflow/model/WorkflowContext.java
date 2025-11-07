package fr.postiqa.core.domain.workflow.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Shared context for data exchange between workflow steps.
 * Acts as a type-safe key-value store for passing data between steps.
 * Immutable by design - each modification returns a new instance.
 */
public record WorkflowContext(Map<String, Object> data) {

    /**
     * Creates an empty workflow context
     */
    public static WorkflowContext empty() {
        return new WorkflowContext(new HashMap<>());
    }

    /**
     * Creates a workflow context with initial data
     */
    public static WorkflowContext of(Map<String, Object> initialData) {
        return new WorkflowContext(new HashMap<>(initialData));
    }

    /**
     * Compact constructor for defensive copying
     */
    public WorkflowContext {
        data = new HashMap<>(data); // Defensive copy
    }

    /**
     * Put a value in the context (returns new instance)
     */
    public WorkflowContext put(String key, Object value) {
        Map<String, Object> newData = new HashMap<>(this.data);
        newData.put(key, value);
        return new WorkflowContext(newData);
    }

    /**
     * Put multiple values in the context (returns new instance)
     */
    public WorkflowContext putAll(Map<String, Object> values) {
        Map<String, Object> newData = new HashMap<>(this.data);
        newData.putAll(values);
        return new WorkflowContext(newData);
    }

    /**
     * Get a value from the context with type casting
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        Object value = data.get(key);
        if (value == null) {
            return Optional.empty();
        }
        if (type.isInstance(value)) {
            return Optional.of((T) value);
        }
        throw new IllegalArgumentException(
            String.format("Value for key '%s' is of type %s, not %s",
                key, value.getClass().getName(), type.getName())
        );
    }

    /**
     * Get a value from the context without type checking
     */
    public Optional<Object> get(String key) {
        return Optional.ofNullable(data.get(key));
    }

    /**
     * Get a required value (throws if not present)
     */
    public <T> T getRequired(String key, Class<T> type) {
        return get(key, type)
            .orElseThrow(() -> new IllegalArgumentException(
                String.format("Required key '%s' not found in workflow context", key)
            ));
    }

    /**
     * Check if context contains a key
     */
    public boolean contains(String key) {
        return data.containsKey(key);
    }

    /**
     * Get the number of entries in the context
     */
    public int size() {
        return data.size();
    }

    /**
     * Check if context is empty
     */
    public boolean isEmpty() {
        return data.isEmpty();
    }

    /**
     * Merge another context into this one (returns new instance)
     */
    public WorkflowContext merge(WorkflowContext other) {
        Map<String, Object> newData = new HashMap<>(this.data);
        newData.putAll(other.data);
        return new WorkflowContext(newData);
    }
}
