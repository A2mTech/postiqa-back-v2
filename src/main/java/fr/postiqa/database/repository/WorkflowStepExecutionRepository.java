package fr.postiqa.database.repository;

import fr.postiqa.core.domain.workflow.enums.StepStatus;
import fr.postiqa.database.entity.WorkflowStepExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for WorkflowStepExecutionEntity.
 */
@Repository
public interface WorkflowStepExecutionRepository extends JpaRepository<WorkflowStepExecutionEntity, String> {

    /**
     * Find all step executions for a workflow instance
     */
    List<WorkflowStepExecutionEntity> findByWorkflowInstanceId(String workflowInstanceId);

    /**
     * Find all step executions for a workflow instance, ordered by creation time
     */
    List<WorkflowStepExecutionEntity> findByWorkflowInstanceIdOrderByCreatedAtAsc(String workflowInstanceId);

    /**
     * Find a specific step execution for a workflow instance
     */
    Optional<WorkflowStepExecutionEntity> findByWorkflowInstanceIdAndStepId(
        String workflowInstanceId,
        String stepId
    );

    /**
     * Find all step executions by status
     */
    List<WorkflowStepExecutionEntity> findByStatus(StepStatus status);

    /**
     * Find all step executions for a workflow instance with a specific status
     */
    List<WorkflowStepExecutionEntity> findByWorkflowInstanceIdAndStatus(
        String workflowInstanceId,
        StepStatus status
    );

    /**
     * Count step executions by workflow instance
     */
    long countByWorkflowInstanceId(String workflowInstanceId);

    /**
     * Count step executions by workflow instance and status
     */
    long countByWorkflowInstanceIdAndStatus(String workflowInstanceId, StepStatus status);

    /**
     * Find all failed step executions for a workflow
     */
    @Query("SELECT s FROM WorkflowStepExecutionEntity s WHERE s.workflowInstanceId = :instanceId " +
           "AND s.status IN ('FAILED', 'TIMED_OUT') ORDER BY s.createdAt ASC")
    List<WorkflowStepExecutionEntity> findFailedSteps(@Param("instanceId") String workflowInstanceId);

    /**
     * Find all completed step executions for a workflow
     */
    @Query("SELECT s FROM WorkflowStepExecutionEntity s WHERE s.workflowInstanceId = :instanceId " +
           "AND s.status = 'COMPLETED' ORDER BY s.completedAt ASC")
    List<WorkflowStepExecutionEntity> findCompletedSteps(@Param("instanceId") String workflowInstanceId);

    /**
     * Find the latest execution for a specific step in a workflow
     */
    @Query("SELECT s FROM WorkflowStepExecutionEntity s WHERE s.workflowInstanceId = :instanceId " +
           "AND s.stepId = :stepId ORDER BY s.attemptNumber DESC LIMIT 1")
    Optional<WorkflowStepExecutionEntity> findLatestAttempt(
        @Param("instanceId") String workflowInstanceId,
        @Param("stepId") String stepId
    );

    /**
     * Find all step executions with a specific step ID across all workflows
     */
    List<WorkflowStepExecutionEntity> findByStepId(String stepId);

    /**
     * Delete all step executions for a workflow instance
     */
    void deleteByWorkflowInstanceId(String workflowInstanceId);
}
