package fr.postiqa.core.domain.workflow.model;

import fr.postiqa.core.domain.workflow.enums.CompensationStrategy;
import fr.postiqa.core.domain.workflow.enums.ExecutionMode;

import java.time.Duration;
import java.util.*;

/**
 * Immutable definition of a workflow.
 * Defines the structure, steps, dependencies, and execution configuration.
 * Uses Builder pattern for fluent construction.
 */
public record WorkflowDefinition(
    String name,
    String description,
    ExecutionMode executionMode,
    CompensationStrategy compensationStrategy,
    Duration globalTimeout,
    Map<String, WorkflowStep<?, ?>> steps,
    Map<String, StepDependency> dependencies
) {

    /**
     * Compact constructor with validation and defensive copies
     */
    public WorkflowDefinition {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Workflow name cannot be null or blank");
        }
        if (executionMode == null) {
            throw new IllegalArgumentException("Execution mode cannot be null");
        }
        if (compensationStrategy == null) {
            throw new IllegalArgumentException("Compensation strategy cannot be null");
        }
        if (globalTimeout == null || globalTimeout.isNegative()) {
            throw new IllegalArgumentException("Global timeout must be positive");
        }

        // Defensive copies
        steps = Map.copyOf(steps);
        dependencies = Map.copyOf(dependencies);

        // Validate: all dependency step IDs must exist in steps
        for (StepDependency dep : dependencies.values()) {
            if (!steps.containsKey(dep.stepId())) {
                throw new IllegalArgumentException(
                    String.format("Step '%s' referenced in dependencies but not defined", dep.stepId())
                );
            }
            for (String dependsOn : dep.dependsOn()) {
                if (!steps.containsKey(dependsOn)) {
                    throw new IllegalArgumentException(
                        String.format("Dependency '%s' referenced but step not defined", dependsOn)
                    );
                }
            }
        }

        // Validate: no circular dependencies (DAG validation)
        validateNoCycles();
    }

    /**
     * Validate that the dependency graph has no cycles
     */
    private void validateNoCycles() {
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (String stepId : steps.keySet()) {
            if (hasCycle(stepId, visited, recursionStack)) {
                throw new IllegalArgumentException(
                    String.format("Circular dependency detected involving step '%s'", stepId)
                );
            }
        }
    }

    /**
     * DFS cycle detection
     */
    private boolean hasCycle(String stepId, Set<String> visited, Set<String> recursionStack) {
        if (recursionStack.contains(stepId)) {
            return true; // Cycle detected
        }
        if (visited.contains(stepId)) {
            return false; // Already checked
        }

        visited.add(stepId);
        recursionStack.add(stepId);

        StepDependency dep = dependencies.get(stepId);
        if (dep != null) {
            for (String dependsOn : dep.dependsOn()) {
                if (hasCycle(dependsOn, visited, recursionStack)) {
                    return true;
                }
            }
        }

        recursionStack.remove(stepId);
        return false;
    }

    /**
     * Get a step by ID
     */
    public Optional<WorkflowStep<?, ?>> getStep(String stepId) {
        return Optional.ofNullable(steps.get(stepId));
    }

    /**
     * Get dependencies for a step
     */
    public Optional<StepDependency> getDependencies(String stepId) {
        return Optional.ofNullable(dependencies.get(stepId));
    }

    /**
     * Get all step IDs
     */
    public Set<String> getStepIds() {
        return steps.keySet();
    }

    /**
     * Get the number of steps
     */
    public int getStepCount() {
        return steps.size();
    }

    /**
     * Check if workflow has parallel execution enabled
     */
    public boolean isParallel() {
        return executionMode == ExecutionMode.PARALLEL;
    }

    /**
     * Check if workflow has compensation enabled
     */
    public boolean hasCompensation() {
        return compensationStrategy.shouldCompensate();
    }

    /**
     * Create a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for fluent workflow definition
     */
    public static class Builder {
        private String name;
        private String description = "";
        private ExecutionMode executionMode = ExecutionMode.SEQUENTIAL;
        private CompensationStrategy compensationStrategy = CompensationStrategy.REVERSE_ORDER;
        private Duration globalTimeout = Duration.ofHours(1);
        private final Map<String, WorkflowStep<?, ?>> steps = new LinkedHashMap<>();
        private final Map<String, StepDependency> dependencies = new LinkedHashMap<>();

        private Builder() {}

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder executionMode(ExecutionMode mode) {
            this.executionMode = mode;
            return this;
        }

        public Builder compensationStrategy(CompensationStrategy strategy) {
            this.compensationStrategy = strategy;
            return this;
        }

        public Builder globalTimeout(Duration timeout) {
            this.globalTimeout = timeout;
            return this;
        }

        /**
         * Add a step to the workflow
         */
        public Builder addStep(WorkflowStep<?, ?> step) {
            this.steps.put(step.getStepId(), step);
            this.dependencies.putIfAbsent(step.getStepId(), StepDependency.noDependencies(step.getStepId()));
            return this;
        }

        /**
         * Add a step with dependencies
         */
        public Builder addStep(WorkflowStep<?, ?> step, String... dependsOn) {
            this.steps.put(step.getStepId(), step);
            this.dependencies.put(step.getStepId(), StepDependency.dependsOn(step.getStepId(), dependsOn));
            return this;
        }

        /**
         * Add a step with dependencies
         */
        public Builder addStep(WorkflowStep<?, ?> step, Set<String> dependsOn) {
            this.steps.put(step.getStepId(), step);
            this.dependencies.put(step.getStepId(), StepDependency.dependsOn(step.getStepId(), dependsOn));
            return this;
        }

        /**
         * Add a dependency between steps
         */
        public Builder addDependency(String stepId, String dependsOn) {
            StepDependency current = dependencies.getOrDefault(
                stepId,
                StepDependency.noDependencies(stepId)
            );
            dependencies.put(stepId, current.addDependency(dependsOn));
            return this;
        }

        /**
         * Build the workflow definition
         */
        public WorkflowDefinition build() {
            return new WorkflowDefinition(
                name,
                description,
                executionMode,
                compensationStrategy,
                globalTimeout,
                steps,
                dependencies
            );
        }
    }
}
