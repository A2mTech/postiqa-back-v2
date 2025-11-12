package fr.postiqa.core.usecase.workflow;

import fr.postiqa.core.domain.workflow.model.WorkflowContext;
import fr.postiqa.core.domain.workflow.model.WorkflowDefinition;
import fr.postiqa.core.domain.workflow.model.WorkflowInstance;
import fr.postiqa.core.domain.workflow.port.WorkflowPersistencePort;
import fr.postiqa.core.infrastructure.workflow.engine.WorkflowEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Use case for starting a new workflow execution.
 * Creates a workflow instance, persists it, and delegates execution to the engine.
 */
@fr.postiqa.shared.annotation.UseCase(
    value = "StartWorkflow",
    resourceType = "WORKFLOW",
    description = "Initializes and starts a workflow execution"
)
@Service
@RequiredArgsConstructor
@Slf4j
public class StartWorkflowUseCase {

    private final WorkflowEngine workflowEngine;
    private final WorkflowPersistencePort persistencePort;

    /**
     * Start a workflow synchronously
     *
     * @param definition The workflow definition
     * @param initialContext Initial context data
     * @return The workflow instance after execution
     */
    public WorkflowInstance execute(WorkflowDefinition definition, WorkflowContext initialContext) {
        log.info("Starting workflow: {}", definition.name());

        // Generate instance ID
        String instanceId = UUID.randomUUID().toString();

        // Create workflow instance
        WorkflowInstance instance = new WorkflowInstance(
            instanceId,
            definition.name(),
            initialContext
        );

        // Persist initial state
        persistencePort.save(instance);

        log.info("Created workflow instance: {}", instanceId);

        // Execute workflow
        return workflowEngine.executeWorkflow(definition, instance);
    }

    /**
     * Start a workflow with empty context
     */
    public WorkflowInstance execute(WorkflowDefinition definition) {
        return execute(definition, WorkflowContext.empty());
    }

    /**
     * Start a workflow asynchronously
     *
     * @param definition The workflow definition
     * @param initialContext Initial context data
     * @return CompletableFuture of the workflow instance
     */
    @Async("workflowExecutor")
    public CompletableFuture<WorkflowInstance> executeAsync(
        WorkflowDefinition definition,
        WorkflowContext initialContext
    ) {
        log.info("Starting workflow asynchronously: {}", definition.name());
        return CompletableFuture.supplyAsync(() -> execute(definition, initialContext));
    }

    /**
     * Start a workflow asynchronously with empty context
     */
    @Async("workflowExecutor")
    public CompletableFuture<WorkflowInstance> executeAsync(WorkflowDefinition definition) {
        return executeAsync(definition, WorkflowContext.empty());
    }

    /**
     * Start a workflow and return only the instance ID (fire-and-forget)
     *
     * @param definition The workflow definition
     * @param initialContext Initial context data
     * @return The instance ID
     */
    public String startAndReturnId(WorkflowDefinition definition, WorkflowContext initialContext) {
        String instanceId = UUID.randomUUID().toString();

        WorkflowInstance instance = new WorkflowInstance(
            instanceId,
            definition.name(),
            initialContext
        );

        persistencePort.save(instance);

        // Execute asynchronously without waiting
        executeAsync(definition, initialContext);

        return instanceId;
    }
}
