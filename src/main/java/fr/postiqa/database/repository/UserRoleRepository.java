package fr.postiqa.database.repository;

import fr.postiqa.database.entity.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for UserRoleEntity.
 * Provides data access methods for user-role associations with scopes.
 */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UUID> {

    /**
     * Find all user roles for a user
     */
    List<UserRoleEntity> findByUserId(UUID userId);

    /**
     * Find all user roles for a role
     */
    List<UserRoleEntity> findByRoleId(UUID roleId);

    /**
     * Find all user roles scoped to an organization
     */
    List<UserRoleEntity> findByOrganizationId(UUID organizationId);

    /**
     * Find all user roles scoped to a client
     */
    List<UserRoleEntity> findByClientId(UUID clientId);

    /**
     * Find all user roles for a user in a specific organization
     */
    List<UserRoleEntity> findByUserIdAndOrganizationId(UUID userId, UUID organizationId);

    /**
     * Find all user roles for a user in a specific client
     */
    List<UserRoleEntity> findByUserIdAndClientId(UUID userId, UUID clientId);
}
