package fr.postiqa.database.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Client entity representing a client managed by an agency.
 * Used for multi-tenant agency module support.
 * NO BUSINESS LOGIC - Pure JPA mapping only.
 */
@Entity
@Table(name = "clients", indexes = {
    @Index(name = "idx_clients_agency_id", columnList = "agency_id"),
    @Index(name = "idx_clients_name", columnList = "client_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id", nullable = false)
    private OrganizationEntity agency;

    @Column(name = "client_name", nullable = false, length = 255)
    private String clientName;

    @Column(name = "client_email", length = 255)
    private String clientEmail;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "client_info", columnDefinition = "jsonb")
    private Map<String, Object> clientInfo;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
