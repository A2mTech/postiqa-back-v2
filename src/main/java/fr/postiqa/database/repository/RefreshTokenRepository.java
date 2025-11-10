package fr.postiqa.database.repository;

import fr.postiqa.database.entity.RefreshTokenEntity;
import fr.postiqa.database.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for RefreshTokenEntity.
 * Provides data access methods for refresh token management.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    /**
     * Find refresh token by token hash
     */
    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    /**
     * Find valid (not revoked, not expired) refresh token by token hash
     */
    @Query("""
        SELECT rt FROM RefreshTokenEntity rt
        WHERE rt.tokenHash = :tokenHash
        AND rt.revoked = false
        AND rt.expiresAt > :now
    """)
    Optional<RefreshTokenEntity> findValidTokenByHash(@Param("tokenHash") String tokenHash, @Param("now") Instant now);

    /**
     * Find all refresh tokens for a user
     */
    List<RefreshTokenEntity> findByUser(UserEntity user);

    /**
     * Find all refresh tokens for a user by user ID
     */
    List<RefreshTokenEntity> findByUserId(UUID userId);

    /**
     * Revoke all refresh tokens for a user
     */
    @Modifying
    @Query("""
        UPDATE RefreshTokenEntity rt
        SET rt.revoked = true, rt.revokedAt = :now
        WHERE rt.user.id = :userId AND rt.revoked = false
    """)
    void revokeAllByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

    /**
     * Delete expired refresh tokens
     */
    @Modifying
    @Query("DELETE FROM RefreshTokenEntity rt WHERE rt.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") Instant now);
}
