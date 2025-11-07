package fr.postiqa.core.domain.workflow.model;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents dependencies between workflow steps.
 * Used to build a Directed Acyclic Graph (DAG) for step execution order.
 */
public record StepDependency(
    String stepId,
    Set<String> dependsOn
) {

    /**
     * Create a step with no dependencies
     */
    public static StepDependency noDependencies(String stepId) {
        return new StepDependency(stepId, Set.of());
    }

    /**
     * Create a step that depends on other steps
     */
    public static StepDependency dependsOn(String stepId, String... dependencies) {
        return new StepDependency(stepId, Set.of(dependencies));
    }

    /**
     * Create a step that depends on other steps
     */
    public static StepDependency dependsOn(String stepId, Set<String> dependencies) {
        return new StepDependency(stepId, dependencies);
    }

    /**
     * Compact constructor with validation and defensive copy
     */
    public StepDependency {
        if (stepId == null || stepId.isBlank()) {
            throw new IllegalArgumentException("stepId cannot be null or blank");
        }
        if (dependsOn == null) {
            throw new IllegalArgumentException("dependsOn cannot be null");
        }
        // Defensive copy
        dependsOn = Set.copyOf(dependsOn);

        // Validate: a step cannot depend on itself
        if (dependsOn.contains(stepId)) {
            throw new IllegalArgumentException(
                String.format("Step '%s' cannot depend on itself", stepId)
            );
        }
    }

    /**
     * Check if this step has no dependencies
     */
    public boolean isRoot() {
        return dependsOn.isEmpty();
    }

    /**
     * Check if this step depends on a specific step
     */
    public boolean dependsOn(String otherStepId) {
        return dependsOn.contains(otherStepId);
    }

    /**
     * Add a new dependency (returns new instance)
     */
    public StepDependency addDependency(String dependency) {
        if (dependency.equals(stepId)) {
            throw new IllegalArgumentException(
                String.format("Step '%s' cannot depend on itself", stepId)
            );
        }
        Set<String> newDependencies = new HashSet<>(dependsOn);
        newDependencies.add(dependency);
        return new StepDependency(stepId, newDependencies);
    }

    /**
     * Remove a dependency (returns new instance)
     */
    public StepDependency removeDependency(String dependency) {
        Set<String> newDependencies = new HashSet<>(dependsOn);
        newDependencies.remove(dependency);
        return new StepDependency(stepId, newDependencies);
    }

    /**
     * Get the number of dependencies
     */
    public int getDependencyCount() {
        return dependsOn.size();
    }
}
