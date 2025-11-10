package fr.postiqa.database.repository;

import fr.postiqa.database.entity.ClientEntity;
import fr.postiqa.database.entity.OrganizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ClientEntity.
 * Provides data access methods for client management (agency multi-tenant).
 */
@Repository
public interface ClientRepository extends JpaRepository<ClientEntity, UUID> {

    /**
     * Find all clients for a specific agency
     */
    List<ClientEntity> findByAgency(OrganizationEntity agency);

    /**
     * Find all clients for a specific agency by ID
     */
    List<ClientEntity> findByAgencyId(UUID agencyId);

    /**
     * Find all active clients for a specific agency
     */
    List<ClientEntity> findByAgencyIdAndActiveTrue(UUID agencyId);

    /**
     * Find client by ID and agency ID (for tenant isolation)
     */
    Optional<ClientEntity> findByIdAndAgencyId(UUID clientId, UUID agencyId);
}
