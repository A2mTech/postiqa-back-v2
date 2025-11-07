package fr.postiqa.database.entity;

import fr.postiqa.core.domain.workflow.enums.WorkflowStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * JPA Entity for persisting workflow instances.
 * Maps to the workflow_instances table.
 */
@Entity
@Table(name = "workflow_instances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowInstanceEntity {

    @Id
    @Column(name = "instance_id", nullable = false, length = 255)
    private String instanceId;

    @Column(name = "workflow_name", nullable = false, length = 255)
    private String workflowName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private WorkflowStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    /**
     * Store workflow context as JSONB (PostgreSQL)
     * This allows efficient querying and indexing of context data
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> context = new HashMap<>();

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    /**
     * Version for optimistic locking
     */
    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        this.updatedAt = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
