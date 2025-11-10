package fr.postiqa.database.repository;

import fr.postiqa.database.entity.EmailVerificationTokenEntity;
import fr.postiqa.database.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for EmailVerificationTokenEntity.
 * Provides data access methods for email verification token management.
 */
@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationTokenEntity, UUID> {

    /**
     * Find email verification token by token
     */
    Optional<EmailVerificationTokenEntity> findByToken(String token);

    /**
     * Find valid (not used, not expired) email verification token by token
     */
    @Query("""
        SELECT evt FROM EmailVerificationTokenEntity evt
        WHERE evt.token = :token
        AND evt.used = false
        AND evt.expiresAt > :now
    """)
    Optional<EmailVerificationTokenEntity> findValidTokenByToken(@Param("token") String token, @Param("now") Instant now);

    /**
     * Find all email verification tokens for a user
     */
    Optional<EmailVerificationTokenEntity> findByUser(UserEntity user);

    /**
     * Delete expired email verification tokens
     */
    @Modifying
    @Query("DELETE FROM EmailVerificationTokenEntity evt WHERE evt.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") Instant now);

    /**
     * Invalidate all unused email verification tokens for a user
     */
    @Modifying
    @Query("""
        UPDATE EmailVerificationTokenEntity evt
        SET evt.used = true, evt.usedAt = :now
        WHERE evt.user.id = :userId AND evt.used = false
    """)
    void invalidateAllByUserId(@Param("userId") UUID userId, @Param("now") Instant now);
}
