package fr.postiqa.database.repository;

import fr.postiqa.database.entity.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for PermissionEntity.
 * Provides data access methods for permission management.
 */
@Repository
public interface PermissionRepository extends JpaRepository<PermissionEntity, UUID> {

    /**
     * Find permission by resource and action
     */
    Optional<PermissionEntity> findByResourceAndAction(String resource, String action);

    /**
     * Find all permissions for a specific resource
     */
    List<PermissionEntity> findByResource(String resource);

    /**
     * Check if permission exists by resource and action
     */
    boolean existsByResourceAndAction(String resource, String action);
}
