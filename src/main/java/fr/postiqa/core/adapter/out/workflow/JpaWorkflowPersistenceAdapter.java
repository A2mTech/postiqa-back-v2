package fr.postiqa.core.adapter.out.workflow;

import fr.postiqa.core.domain.workflow.enums.StepStatus;
import fr.postiqa.core.domain.workflow.enums.WorkflowStatus;
import fr.postiqa.core.domain.workflow.model.WorkflowContext;
import fr.postiqa.core.domain.workflow.model.WorkflowInstance;
import fr.postiqa.core.domain.workflow.port.WorkflowPersistencePort;
import fr.postiqa.database.entity.WorkflowInstanceEntity;
import fr.postiqa.database.entity.WorkflowStepExecutionEntity;
import fr.postiqa.database.repository.WorkflowInstanceRepository;
import fr.postiqa.database.repository.WorkflowStepExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * JPA implementation of WorkflowPersistencePort.
 * Persists workflow instances and step executions to PostgreSQL.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JpaWorkflowPersistenceAdapter implements WorkflowPersistencePort {

    private final WorkflowInstanceRepository instanceRepository;
    private final WorkflowStepExecutionRepository stepExecutionRepository;

    @Override
    @Transactional
    public WorkflowInstance save(WorkflowInstance instance) {
        log.debug("Saving workflow instance: {}", instance.getInstanceId());

        // Convert domain model to entity
        WorkflowInstanceEntity entity = toEntity(instance);

        // Save entity
        instanceRepository.save(entity);

        // Save step executions
        for (WorkflowInstance.StepExecution stepExec : instance.getStepExecutions().values()) {
            WorkflowStepExecutionEntity stepEntity = toStepExecutionEntity(stepExec, instance.getInstanceId());
            stepExecutionRepository.save(stepEntity);
        }

        log.debug("Successfully saved workflow instance: {}", instance.getInstanceId());
        return instance;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WorkflowInstance> findById(String instanceId) {
        log.debug("Finding workflow instance by ID: {}", instanceId);

        return instanceRepository.findById(instanceId)
            .map(this::toDomainModel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowInstance> findByWorkflowName(String workflowName) {
        log.debug("Finding workflow instances by name: {}", workflowName);

        return instanceRepository.findByWorkflowName(workflowName)
            .stream()
            .map(this::toDomainModel)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowInstance> findByStatus(WorkflowStatus status) {
        log.debug("Finding workflow instances by status: {}", status);

        return instanceRepository.findByStatus(status)
            .stream()
            .map(this::toDomainModel)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowInstance> findByWorkflowNameAndStatus(String workflowName, WorkflowStatus status) {
        log.debug("Finding workflow instances by name {} and status: {}", workflowName, status);

        return instanceRepository.findByWorkflowNameAndStatus(workflowName, status)
            .stream()
            .map(this::toDomainModel)
            .toList();
    }

    @Override
    @Transactional
    public boolean deleteById(String instanceId) {
        log.debug("Deleting workflow instance: {}", instanceId);

        if (!instanceRepository.existsById(instanceId)) {
            return false;
        }

        // Delete step executions first (cascade should handle this, but being explicit)
        stepExecutionRepository.deleteByWorkflowInstanceId(instanceId);

        // Delete instance
        instanceRepository.deleteById(instanceId);

        log.debug("Successfully deleted workflow instance: {}", instanceId);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(String instanceId) {
        return instanceRepository.existsById(instanceId);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return instanceRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(WorkflowStatus status) {
        return instanceRepository.countByStatus(status);
    }

    /**
     * Convert domain model to entity
     */
    private WorkflowInstanceEntity toEntity(WorkflowInstance instance) {
        return WorkflowInstanceEntity.builder()
            .instanceId(instance.getInstanceId())
            .workflowName(instance.getWorkflowName())
            .status(instance.getStatus())
            .createdAt(instance.getCreatedAt())
            .startedAt(instance.getStartedAt().orElse(null))
            .completedAt(instance.getCompletedAt().orElse(null))
            .context(instance.getContext().data())
            .failureReason(instance.getFailureReason().orElse(null))
            .build();
    }

    /**
     * Convert entity to domain model
     */
    private WorkflowInstance toDomainModel(WorkflowInstanceEntity entity) {
        WorkflowContext context = WorkflowContext.of(
            entity.getContext() != null ? entity.getContext() : new HashMap<>()
        );

        WorkflowInstance instance = new WorkflowInstance(
            entity.getInstanceId(),
            entity.getWorkflowName(),
            context
        );

        // Restore state (using reflection to set private fields - not ideal but necessary)
        restoreInstanceState(instance, entity);

        // Load step executions
        List<WorkflowStepExecutionEntity> stepExecutions =
            stepExecutionRepository.findByWorkflowInstanceId(entity.getInstanceId());

        for (WorkflowStepExecutionEntity stepExec : stepExecutions) {
            WorkflowInstance.StepExecution domainStepExec = toStepExecutionDomain(stepExec);
            instance.recordStepExecution(stepExec.getStepId(), domainStepExec);
        }

        return instance;
    }

    /**
     * Restore instance state from entity
     */
    private void restoreInstanceState(WorkflowInstance instance, WorkflowInstanceEntity entity) {
        // Set status based on entity
        switch (entity.getStatus()) {
            case RUNNING -> {
                if (instance.getStatus() == WorkflowStatus.PENDING) {
                    instance.start();
                }
            }
            case COMPLETED -> {
                if (instance.getStatus() != WorkflowStatus.COMPLETED) {
                    if (instance.getStatus() == WorkflowStatus.PENDING) {
                        instance.start();
                    }
                    instance.complete();
                }
            }
            case FAILED -> {
                if (instance.getStatus() != WorkflowStatus.FAILED) {
                    instance.fail(entity.getFailureReason());
                }
            }
            case PAUSED -> {
                if (instance.getStatus() == WorkflowStatus.PENDING) {
                    instance.start();
                }
                if (instance.getStatus() == WorkflowStatus.RUNNING) {
                    instance.pause();
                }
            }
            case COMPENSATING, COMPENSATED -> {
                // Handle compensation states
                if (entity.getStatus() == WorkflowStatus.COMPENSATED) {
                    instance.fail(entity.getFailureReason());
                    instance.startCompensation();
                    instance.completeCompensation();
                }
            }
            case CANCELLED -> instance.cancel();
        }
    }

    /**
     * Convert step execution domain model to entity
     */
    private WorkflowStepExecutionEntity toStepExecutionEntity(
        WorkflowInstance.StepExecution stepExec,
        String instanceId
    ) {
        return WorkflowStepExecutionEntity.builder()
            .workflowInstanceId(instanceId)
            .stepId(stepExec.stepId())
            .status(stepExec.status())
            .startedAt(stepExec.startedAt())
            .completedAt(stepExec.completedAt())
            .attemptNumber(stepExec.attemptNumber())
            .errorMessage(stepExec.errorMessage().orElse(null))
            .output(stepExec.output().orElse(null))
            .build();
    }

    /**
     * Convert step execution entity to domain model
     */
    private WorkflowInstance.StepExecution toStepExecutionDomain(WorkflowStepExecutionEntity entity) {
        return new WorkflowInstance.StepExecution(
            entity.getStepId(),
            entity.getStatus(),
            entity.getStartedAt(),
            entity.getCompletedAt(),
            entity.getAttemptNumber(),
            Optional.ofNullable(entity.getErrorMessage()),
            Optional.ofNullable(entity.getOutput())
        );
    }
}
