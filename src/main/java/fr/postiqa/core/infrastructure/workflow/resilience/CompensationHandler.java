package fr.postiqa.core.infrastructure.workflow.resilience;

import fr.postiqa.core.domain.workflow.enums.CompensationStrategy;
import fr.postiqa.core.domain.workflow.model.CompensationAction;
import fr.postiqa.core.domain.workflow.model.WorkflowContext;
import fr.postiqa.core.domain.workflow.model.WorkflowInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Handles compensation (rollback) logic for failed workflows.
 * Executes compensation actions in reverse order (Saga pattern).
 */
@Component
@Slf4j
public class CompensationHandler {

    /**
     * Execute compensation for completed steps
     *
     * @param completedSteps List of completed steps with their outputs
     * @param compensationActions Map of step ID to compensation action
     * @param strategy The compensation strategy to use
     * @param context The workflow context
     * @return true if all compensations succeeded, false otherwise
     */
    public boolean executeCompensation(
        List<CompensatedStep> completedSteps,
        Map<String, CompensationAction<?>> compensationActions,
        CompensationStrategy strategy,
        WorkflowContext context
    ) {
        if (strategy == CompensationStrategy.NONE) {
            log.info("Compensation strategy is NONE, skipping compensation");
            return true;
        }

        log.info("Executing compensation for {} completed steps using strategy: {}",
            completedSteps.size(), strategy);

        List<CompensatedStep> stepsToCompensate = new ArrayList<>(completedSteps);

        if (strategy == CompensationStrategy.REVERSE_ORDER) {
            // Reverse order (LIFO - most recent first)
            Collections.reverse(stepsToCompensate);
        }

        boolean allSucceeded = true;
        int successCount = 0;
        int failureCount = 0;

        for (CompensatedStep step : stepsToCompensate) {
            CompensationAction compensationAction = compensationActions.get(step.stepId);

            if (compensationAction == null) {
                log.debug("No compensation action defined for step: {}, skipping", step.stepId);
                continue;
            }

            try {
                log.info("Compensating step: {}", step.stepId);
                compensationAction.compensate(step.output, context);
                successCount++;
                log.info("Successfully compensated step: {}", step.stepId);

            } catch (Exception e) {
                failureCount++;
                allSucceeded = false;
                log.error("Failed to compensate step: {}", step.stepId, e);

                // Depending on strategy, we might stop or continue
                if (strategy == CompensationStrategy.REVERSE_ORDER) {
                    // Continue compensating remaining steps even if one fails
                    log.warn("Continuing compensation despite failure in step: {}", step.stepId);
                }
            }
        }

        log.info("Compensation complete. Success: {}, Failures: {}",
            successCount, failureCount);

        return allSucceeded;
    }

    /**
     * Execute compensation for a single step
     *
     * @param stepId The step ID
     * @param output The step output
     * @param compensationAction The compensation action
     * @param context The workflow context
     * @return true if compensation succeeded
     */
    @SuppressWarnings("unchecked")
    public <O> boolean compensateStep(
        String stepId,
        O output,
        CompensationAction<O> compensationAction,
        WorkflowContext context
    ) {
        if (compensationAction == null) {
            log.debug("No compensation action for step: {}", stepId);
            return true;
        }

        try {
            log.info("Compensating step: {}", stepId);
            compensationAction.compensate(output, context);
            log.info("Successfully compensated step: {}", stepId);
            return true;

        } catch (Exception e) {
            log.error("Failed to compensate step: {}", stepId, e);
            return false;
        }
    }

    /**
     * Record for tracking completed steps with their outputs for compensation
     */
    public record CompensatedStep(
        String stepId,
        Object output,
        long completedAtMillis
    ) {
        public static CompensatedStep of(String stepId, Object output) {
            return new CompensatedStep(stepId, output, System.currentTimeMillis());
        }
    }
}
