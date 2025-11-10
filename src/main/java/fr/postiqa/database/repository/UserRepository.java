package fr.postiqa.database.repository;

import fr.postiqa.database.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserEntity.
 * Provides data access methods for user authentication and management.
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    /**
     * Find user by email (case-insensitive)
     */
    Optional<UserEntity> findByEmailIgnoreCase(String email);

    /**
     * Check if email exists (case-insensitive)
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Find user by email with eager-loaded roles and permissions for authentication
     */
    @Query("""
        SELECT DISTINCT u FROM UserEntity u
        LEFT JOIN FETCH u.userRoles ur
        LEFT JOIN FETCH ur.role r
        LEFT JOIN FETCH r.rolePermissions rp
        LEFT JOIN FETCH rp.permission p
        LEFT JOIN FETCH ur.organization
        LEFT JOIN FETCH ur.client
        WHERE LOWER(u.email) = LOWER(:email)
    """)
    Optional<UserEntity> findByEmailWithRolesAndPermissions(@Param("email") String email);
}
