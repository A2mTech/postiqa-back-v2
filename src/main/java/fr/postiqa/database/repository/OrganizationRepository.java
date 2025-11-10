package fr.postiqa.database.repository;

import fr.postiqa.database.entity.OrganizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for OrganizationEntity.
 * Provides data access methods for organization management.
 */
@Repository
public interface OrganizationRepository extends JpaRepository<OrganizationEntity, UUID> {

    /**
     * Find all organizations by type
     */
    List<OrganizationEntity> findByType(OrganizationEntity.OrganizationType type);

    /**
     * Find all active organizations
     */
    List<OrganizationEntity> findByActiveTrue();

    /**
     * Find all active organizations by type
     */
    List<OrganizationEntity> findByTypeAndActiveTrue(OrganizationEntity.OrganizationType type);
}
