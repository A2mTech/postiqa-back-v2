package fr.postiqa.database.entity;

import fr.postiqa.core.domain.workflow.enums.StepStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * JPA Entity for persisting workflow step executions.
 * Maps to the workflow_step_executions table.
 */
@Entity
@Table(
    name = "workflow_step_executions",
    indexes = {
        @Index(name = "idx_workflow_instance", columnList = "workflow_instance_id"),
        @Index(name = "idx_step_status", columnList = "status"),
        @Index(name = "idx_workflow_step", columnList = "workflow_instance_id,step_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStepExecutionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "execution_id", nullable = false)
    private String executionId;

    @Column(name = "workflow_instance_id", nullable = false, length = 255)
    private String workflowInstanceId;

    @Column(name = "step_id", nullable = false, length = 255)
    private String stepId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private StepStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Store step output as JSONB (PostgreSQL)
     * Allows storing complex output objects
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output", columnDefinition = "jsonb")
    private Object output;

    /**
     * Store additional execution metadata as JSONB
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Object metadata;

    /**
     * Foreign key relationship to workflow instance
     * Cascade: when workflow instance is deleted, delete all step executions
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_instance_id", insertable = false, updatable = false)
    private WorkflowInstanceEntity workflowInstance;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
