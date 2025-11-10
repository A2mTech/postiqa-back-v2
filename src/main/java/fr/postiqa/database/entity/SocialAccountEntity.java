package fr.postiqa.database.entity;

import fr.postiqa.shared.enums.AccountType;
import fr.postiqa.shared.enums.SocialPlatform;
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
 * SocialAccount entity for storing connected social media accounts.
 * Supports multiple accounts per platform for business/agency users.
 * Stores OAuth2 tokens for publishing content to social networks.
 * NO BUSINESS LOGIC - Pure JPA mapping only.
 */
@Entity
@Table(name = "social_accounts", indexes = {
    @Index(name = "idx_social_accounts_user_id", columnList = "user_id"),
    @Index(name = "idx_social_accounts_organization_id", columnList = "organization_id"),
    @Index(name = "idx_social_accounts_client_id", columnList = "client_id"),
    @Index(name = "idx_social_accounts_platform", columnList = "platform"),
    @Index(name = "idx_social_accounts_active", columnList = "active"),
    @Index(name = "idx_social_accounts_token_expires_at", columnList = "token_expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationEntity organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private ClientEntity client;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SocialPlatform platform;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    @Builder.Default
    private AccountType accountType = AccountType.BUSINESS;

    @Column(name = "platform_account_id", nullable = false, length = 255)
    private String platformAccountId;

    @Column(name = "account_name", length = 255)
    private String accountName;

    @Column(name = "account_handle", length = 255)
    private String accountHandle;

    @Column(name = "account_avatar_url", length = 500)
    private String accountAvatarUrl;

    @Column(name = "access_token", columnDefinition = "TEXT", nullable = false)
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;

    @Column(name = "scopes", columnDefinition = "TEXT")
    private String scopes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "platform_metadata", columnDefinition = "jsonb")
    private Map<String, Object> platformMetadata;

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
