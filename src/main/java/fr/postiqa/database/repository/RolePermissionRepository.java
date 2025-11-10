package fr.postiqa.database.repository;

import fr.postiqa.database.entity.RolePermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for RolePermissionEntity.
 * Provides data access methods for role-permission associations.
 */
@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermissionEntity, UUID> {

    /**
     * Find all role-permission mappings for a role
     */
    List<RolePermissionEntity> findByRoleId(UUID roleId);

    /**
     * Find all role-permission mappings for a permission
     */
    List<RolePermissionEntity> findByPermissionId(UUID permissionId);

    /**
     * Delete all role-permission mappings for a role
     */
    void deleteByRoleId(UUID roleId);

    /**
     * Delete all role-permission mappings for a permission
     */
    void deleteByPermissionId(UUID permissionId);
}
