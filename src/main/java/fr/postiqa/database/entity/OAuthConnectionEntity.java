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
 * OAuthConnection entity for OAuth2 social login connections.
 * Links users to their OAuth2 providers (Google, LinkedIn, etc.)
 * NO BUSINESS LOGIC - Pure JPA mapping only.
 */
@Entity
@Table(name = "oauth_connections", indexes = {
    @Index(name = "idx_oauth_connections_user_id", columnList = "user_id"),
    @Index(name = "idx_oauth_connections_provider", columnList = "provider"),
    @Index(name = "idx_oauth_connections_provider_user_id", columnList = "provider, provider_user_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthConnectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Column(name = "provider_email", length = 255)
    private String providerEmail;

    @Column(name = "provider_name", length = 255)
    private String providerName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "provider_data", columnDefinition = "jsonb")
    private Map<String, Object> providerData;

    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
