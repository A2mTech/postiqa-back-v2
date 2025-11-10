package fr.postiqa.database.repository;

import fr.postiqa.database.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for RoleEntity.
 * Provides data access methods for role management.
 */
@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {

    /**
     * Find role by name
     */
    Optional<RoleEntity> findByName(String name);

    /**
     * Check if role exists by name
     */
    boolean existsByName(String name);

    /**
     * Find role by name with eager-loaded permissions
     */
    @Query("""
        SELECT DISTINCT r FROM RoleEntity r
        LEFT JOIN FETCH r.rolePermissions rp
        LEFT JOIN FETCH rp.permission p
        WHERE r.name = :name
    """)
    Optional<RoleEntity> findByNameWithPermissions(@Param("name") String name);
}
