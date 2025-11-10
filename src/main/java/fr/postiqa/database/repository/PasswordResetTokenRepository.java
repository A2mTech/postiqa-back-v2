package fr.postiqa.database.repository;

import fr.postiqa.database.entity.PasswordResetTokenEntity;
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
 * Repository for PasswordResetTokenEntity.
 * Provides data access methods for password reset token management.
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, UUID> {

    /**
     * Find password reset token by token
     */
    Optional<PasswordResetTokenEntity> findByToken(String token);

    /**
     * Find valid (not used, not expired) password reset token by token
     */
    @Query("""
        SELECT prt FROM PasswordResetTokenEntity prt
        WHERE prt.token = :token
        AND prt.used = false
        AND prt.expiresAt > :now
    """)
    Optional<PasswordResetTokenEntity> findValidTokenByToken(@Param("token") String token, @Param("now") Instant now);

    /**
     * Find all password reset tokens for a user
     */
    Optional<PasswordResetTokenEntity> findByUser(UserEntity user);

    /**
     * Delete expired password reset tokens
     */
    @Modifying
    @Query("DELETE FROM PasswordResetTokenEntity prt WHERE prt.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") Instant now);

    /**
     * Invalidate all unused password reset tokens for a user
     */
    @Modifying
    @Query("""
        UPDATE PasswordResetTokenEntity prt
        SET prt.used = true, prt.usedAt = :now
        WHERE prt.user.id = :userId AND prt.used = false
    """)
    void invalidateAllByUserId(@Param("userId") UUID userId, @Param("now") Instant now);
}
