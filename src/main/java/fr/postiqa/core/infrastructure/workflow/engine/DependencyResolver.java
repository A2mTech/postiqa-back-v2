package fr.postiqa.core.infrastructure.workflow.engine;

import fr.postiqa.core.domain.workflow.model.StepDependency;
import fr.postiqa.core.domain.workflow.model.WorkflowDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Resolves the execution order of workflow steps based on their dependencies.
 * Implements topological sort algorithm (Kahn's algorithm) to determine the order.
 */
@Component
@Slf4j
public class DependencyResolver {

    /**
     * Resolve execution order for all steps in a workflow.
     * Returns a list of "layers" where each layer contains steps that can be executed in parallel.
     *
     * @param definition The workflow definition
     * @return List of execution layers (each layer = parallel executable steps)
     */
    public List<Set<String>> resolveExecutionOrder(WorkflowDefinition definition) {
        log.debug("Resolving execution order for workflow: {}", definition.name());

        Map<String, Set<String>> dependencies = buildDependencyMap(definition);
        Map<String, Integer> inDegree = calculateInDegree(dependencies);

        List<Set<String>> executionLayers = new ArrayList<>();
        Set<String> processed = new HashSet<>();

        while (processed.size() < definition.getStepCount()) {
            // Find all steps with no pending dependencies
            Set<String> currentLayer = new HashSet<>();
            for (String stepId : definition.getStepIds()) {
                if (!processed.contains(stepId) && inDegree.get(stepId) == 0) {
                    currentLayer.add(stepId);
                }
            }

            if (currentLayer.isEmpty()) {
                throw new IllegalStateException(
                    "Deadlock detected: circular dependencies or disconnected graph"
                );
            }

            executionLayers.add(currentLayer);
            processed.addAll(currentLayer);

            // Update in-degrees for next iteration
            for (String completedStep : currentLayer) {
                for (String dependentStep : getDependents(completedStep, dependencies)) {
                    inDegree.put(dependentStep, inDegree.get(dependentStep) - 1);
                }
            }
        }

        log.debug("Resolved {} execution layers for workflow {}", executionLayers.size(), definition.name());
        return executionLayers;
    }

    /**
     * Check if all dependencies for a step are satisfied
     *
     * @param stepId The step to check
     * @param completedSteps Set of completed step IDs
     * @param definition The workflow definition
     * @return true if all dependencies are satisfied
     */
    public boolean areDependenciesSatisfied(
        String stepId,
        Set<String> completedSteps,
        WorkflowDefinition definition
    ) {
        Optional<StepDependency> dependency = definition.getDependencies(stepId);
        if (dependency.isEmpty()) {
            return true; // No dependencies
        }

        Set<String> requiredSteps = dependency.get().dependsOn();
        return completedSteps.containsAll(requiredSteps);
    }

    /**
     * Get all steps that can be executed given the current completed steps
     *
     * @param completedSteps Set of completed step IDs
     * @param definition The workflow definition
     * @return Set of step IDs that can be executed now
     */
    public Set<String> getExecutableSteps(
        Set<String> completedSteps,
        WorkflowDefinition definition
    ) {
        Set<String> executable = new HashSet<>();

        for (String stepId : definition.getStepIds()) {
            if (!completedSteps.contains(stepId) &&
                areDependenciesSatisfied(stepId, completedSteps, definition)) {
                executable.add(stepId);
            }
        }

        return executable;
    }

    /**
     * Get the root steps (steps with no dependencies)
     *
     * @param definition The workflow definition
     * @return Set of root step IDs
     */
    public Set<String> getRootSteps(WorkflowDefinition definition) {
        Set<String> roots = new HashSet<>();

        for (String stepId : definition.getStepIds()) {
            Optional<StepDependency> dependency = definition.getDependencies(stepId);
            if (dependency.isEmpty() || dependency.get().isRoot()) {
                roots.add(stepId);
            }
        }

        return roots;
    }

    /**
     * Build a dependency map (stepId -> set of steps it depends on)
     */
    private Map<String, Set<String>> buildDependencyMap(WorkflowDefinition definition) {
        Map<String, Set<String>> depMap = new HashMap<>();

        for (String stepId : definition.getStepIds()) {
            Optional<StepDependency> dependency = definition.getDependencies(stepId);
            depMap.put(stepId, dependency.map(StepDependency::dependsOn).orElse(Set.of()));
        }

        return depMap;
    }

    /**
     * Calculate in-degree for each step (number of dependencies)
     */
    private Map<String, Integer> calculateInDegree(Map<String, Set<String>> dependencies) {
        Map<String, Integer> inDegree = new HashMap<>();

        // Initialize all to 0
        for (String stepId : dependencies.keySet()) {
            inDegree.put(stepId, 0);
        }

        // Count dependencies
        for (Set<String> deps : dependencies.values()) {
            for (String dep : deps) {
                inDegree.put(dep, inDegree.getOrDefault(dep, 0));
            }
        }

        // Count how many steps depend on each step
        for (String stepId : dependencies.keySet()) {
            inDegree.put(stepId, dependencies.get(stepId).size());
        }

        return inDegree;
    }

    /**
     * Get all steps that depend on a given step
     */
    private Set<String> getDependents(String stepId, Map<String, Set<String>> dependencies) {
        Set<String> dependents = new HashSet<>();

        for (Map.Entry<String, Set<String>> entry : dependencies.entrySet()) {
            if (entry.getValue().contains(stepId)) {
                dependents.add(entry.getKey());
            }
        }

        return dependents;
    }
}
