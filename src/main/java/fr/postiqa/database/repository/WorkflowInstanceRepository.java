package fr.postiqa.database.repository;

import fr.postiqa.core.domain.workflow.enums.WorkflowStatus;
import fr.postiqa.database.entity.WorkflowInstanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Spring Data JPA repository for WorkflowInstanceEntity.
 */
@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstanceEntity, String> {

    /**
     * Find all workflow instances by workflow name
     */
    List<WorkflowInstanceEntity> findByWorkflowName(String workflowName);

    /**
     * Find all workflow instances by status
     */
    List<WorkflowInstanceEntity> findByStatus(WorkflowStatus status);

    /**
     * Find all workflow instances by workflow name and status
     */
    List<WorkflowInstanceEntity> findByWorkflowNameAndStatus(String workflowName, WorkflowStatus status);

    /**
     * Count workflow instances by status
     */
    long countByStatus(WorkflowStatus status);

    /**
     * Find all running workflows (not terminal states)
     */
    @Query("SELECT w FROM WorkflowInstanceEntity w WHERE w.status IN ('RUNNING', 'PAUSED', 'COMPENSATING')")
    List<WorkflowInstanceEntity> findAllActive();

    /**
     * Find all paused workflows that can be resumed
     */
    @Query("SELECT w FROM WorkflowInstanceEntity w WHERE w.status = 'PAUSED'")
    List<WorkflowInstanceEntity> findAllPaused();

    /**
     * Find workflows created after a specific timestamp
     */
    List<WorkflowInstanceEntity> findByCreatedAtAfter(Instant timestamp);

    /**
     * Find workflows completed after a specific timestamp
     */
    List<WorkflowInstanceEntity> findByCompletedAtAfter(Instant timestamp);

    /**
     * Find failed workflows with failure reason matching pattern
     */
    @Query("SELECT w FROM WorkflowInstanceEntity w WHERE w.status = 'FAILED' AND w.failureReason LIKE %:pattern%")
    List<WorkflowInstanceEntity> findFailedWorkflowsWithReason(@Param("pattern") String pattern);

    /**
     * Count workflows by workflow name and status
     */
    long countByWorkflowNameAndStatus(String workflowName, WorkflowStatus status);
}
