package fr.postiqa.features.socialaccounts.adapter.out;

import fr.postiqa.database.entity.ClientEntity;
import fr.postiqa.database.entity.OrganizationEntity;
import fr.postiqa.database.entity.SocialAccountEntity;
import fr.postiqa.database.entity.UserEntity;
import fr.postiqa.database.repository.ClientRepository;
import fr.postiqa.database.repository.OrganizationRepository;
import fr.postiqa.database.repository.SocialAccountRepository;
import fr.postiqa.database.repository.UserRepository;
import fr.postiqa.features.socialaccounts.domain.model.OAuth2Token;
import fr.postiqa.features.socialaccounts.domain.model.SocialAccount;
import fr.postiqa.features.socialaccounts.domain.port.SocialAccountPort;
import fr.postiqa.shared.enums.AccountType;
import fr.postiqa.shared.enums.SocialPlatform;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adapter implementing SocialAccountPort using Spring Data repository.
 * Maps between domain models and JPA entities.
 */
@Component
@RequiredArgsConstructor
public class SocialAccountRepositoryAdapter implements SocialAccountPort {

    private final SocialAccountRepository repository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final ClientRepository clientRepository;

    @Override
    public SocialAccount save(SocialAccount account) {
        SocialAccountEntity entity = toEntity(account);
        SocialAccountEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<SocialAccount> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<SocialAccount> findByIdAndOrganizationId(UUID id, UUID organizationId) {
        return repository.findByIdAndOrganizationId(id, organizationId).map(this::toDomain);
    }

    @Override
    public Optional<SocialAccount> findByIdAndClientId(UUID id, UUID clientId) {
        return repository.findByIdAndClientId(id, clientId).map(this::toDomain);
    }

    @Override
    public List<SocialAccount> findByOrganizationId(UUID organizationId) {
        return repository.findByOrganizationId(organizationId)
            .stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<SocialAccount> findActiveByOrganizationId(UUID organizationId) {
        return repository.findByOrganizationIdAndActiveTrue(organizationId)
            .stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<SocialAccount> findByClientId(UUID clientId) {
        return repository.findByClientId(clientId)
            .stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<SocialAccount> findActiveByClientId(UUID clientId) {
        return repository.findByClientIdAndActiveTrue(clientId)
            .stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<SocialAccount> findByOrganizationAndPlatformAndPlatformAccountId(
        UUID organizationId,
        SocialPlatform platform,
        String platformAccountId
    ) {
        return repository.findByOrganizationIdAndPlatformAndPlatformAccountId(
            organizationId, platform, platformAccountId
        ).map(this::toDomain);
    }

    @Override
    public Optional<SocialAccount> findByClientAndPlatformAndPlatformAccountId(
        UUID clientId,
        SocialPlatform platform,
        String platformAccountId
    ) {
        return repository.findByClientIdAndPlatformAndPlatformAccountId(
            clientId, platform, platformAccountId
        ).map(this::toDomain);
    }

    @Override
    public List<SocialAccount> findAccountsWithExpiringTokens(Instant expirationThreshold) {
        return repository.findAccountsWithExpiringTokens(expirationThreshold)
            .stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<SocialAccount> findAccountsWithExpiredTokens(Instant now) {
        return repository.findAccountsWithExpiredTokens(now)
            .stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public void delete(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public boolean existsByOrganizationAndPlatformAndPlatformAccountId(
        UUID organizationId,
        SocialPlatform platform,
        String platformAccountId
    ) {
        return repository.existsByOrganizationIdAndPlatformAndPlatformAccountId(
            organizationId, platform, platformAccountId
        );
    }

    @Override
    public long countByOrganizationId(UUID organizationId) {
        return repository.countByOrganizationId(organizationId);
    }

    @Override
    public long countByClientId(UUID clientId) {
        return repository.countByClientId(clientId);
    }

    // Mapping methods

    private SocialAccount toDomain(SocialAccountEntity entity) {
        OAuth2Token token = OAuth2Token.builder()
            .accessToken(entity.getAccessToken())
            .refreshToken(entity.getRefreshToken())
            .expiresAt(entity.getTokenExpiresAt())
            .scope(entity.getScopes())
            .build();

        return SocialAccount.builder()
            .id(entity.getId())
            .userId(entity.getUser().getId())
            .organizationId(entity.getOrganization().getId())
            .clientId(entity.getClient() != null ? entity.getClient().getId() : null)
            .platform(entity.getPlatform())
            .platformAccountId(entity.getPlatformAccountId())
            .accountName(entity.getAccountName())
            .accountHandle(entity.getAccountHandle())
            .accountAvatarUrl(entity.getAccountAvatarUrl())
            .token(token)
            .scopes(entity.getScopes())
            .platformMetadata(entity.getPlatformMetadata())
            .active(entity.getActive())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    private SocialAccountEntity toEntity(SocialAccount domain) {
        // Load or fetch existing entity if ID exists
        SocialAccountEntity entity = domain.getId() != null
            ? repository.findById(domain.getId()).orElse(new SocialAccountEntity())
            : new SocialAccountEntity();

        entity.setId(domain.getId());

        // Load User, Organization, and Client entities
        UserEntity user = userRepository.findById(domain.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found: " + domain.getUserId()));
        entity.setUser(user);

        OrganizationEntity organization = organizationRepository.findById(domain.getOrganizationId())
            .orElseThrow(() -> new RuntimeException("Organization not found: " + domain.getOrganizationId()));
        entity.setOrganization(organization);

        if (domain.getClientId() != null) {
            ClientEntity client = clientRepository.findById(domain.getClientId())
                .orElseThrow(() -> new RuntimeException("Client not found: " + domain.getClientId()));
            entity.setClient(client);
        } else {
            entity.setClient(null);
        }

        // AccountType represents the type of social media account (BUSINESS/PERSONAL)
        // not whether it's managed by an agency. Default to BUSINESS for now.
        entity.setAccountType(AccountType.BUSINESS);

        entity.setPlatform(domain.getPlatform());
        entity.setPlatformAccountId(domain.getPlatformAccountId());
        entity.setAccountName(domain.getAccountName());
        entity.setAccountHandle(domain.getAccountHandle());
        entity.setAccountAvatarUrl(domain.getAccountAvatarUrl());

        if (domain.getToken() != null) {
            entity.setAccessToken(domain.getToken().getAccessToken());
            entity.setRefreshToken(domain.getToken().getRefreshToken());
            entity.setTokenExpiresAt(domain.getToken().getExpiresAt());
        }

        entity.setScopes(domain.getScopes());
        entity.setPlatformMetadata(domain.getPlatformMetadata());
        entity.setActive(domain.getActive());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());

        return entity;
    }
}
