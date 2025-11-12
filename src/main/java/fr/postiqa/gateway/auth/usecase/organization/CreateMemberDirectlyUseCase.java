package fr.postiqa.gateway.auth.usecase.organization;

import fr.postiqa.database.entity.OrganizationMemberEntity;
import fr.postiqa.database.entity.UserEntity;
import fr.postiqa.database.entity.UserRoleEntity;
import fr.postiqa.database.repository.ClientRepository;
import fr.postiqa.database.repository.OrganizationRepository;
import fr.postiqa.database.repository.RoleRepository;
import fr.postiqa.database.repository.UserRepository;
import fr.postiqa.database.repository.UserRoleRepository;
import fr.postiqa.gateway.auth.service.OrganizationMemberService;
import fr.postiqa.shared.annotation.UseCase;
import fr.postiqa.shared.dto.auth.CreateMemberRequest;
import fr.postiqa.shared.dto.auth.UserDto;
import fr.postiqa.shared.exception.auth.OrganizationNotFoundException;
import fr.postiqa.shared.exception.auth.UserAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Use case for creating a member directly without invitation.
 */
@UseCase(
    value = "CreateMemberDirectly",
    resourceType = "MEMBER",
    description = "Creates a member directly without invitation"
)
@Service
@RequiredArgsConstructor
@Slf4j
public class CreateMemberDirectlyUseCase implements fr.postiqa.shared.usecase.UseCase<CreateMemberRequest, UserDto> {

    private final OrganizationMemberService memberService;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserDto execute(CreateMemberRequest request) {
        // Get createdByUserId from tenant context
        UUID createdByUserId = fr.postiqa.gateway.auth.authorization.TenantContextHolder.getUserId();
        // Validate organization exists
        var organization = organizationRepository.findById(request.getOrganizationId())
            .orElseThrow(() -> new OrganizationNotFoundException("Organization not found"));

        // Validate role exists
        var role = roleRepository.findById(request.getRoleId())
            .orElseThrow(() -> new RuntimeException("Role not found"));

        // Check if user already exists
        Optional<UserEntity> existingUser = userRepository.findByEmailIgnoreCase(request.getEmail());

        if (existingUser.isPresent()) {
            throw new UserAlreadyExistsException("User with this email already exists");
        }

        // Create new user
        UserEntity user = UserEntity.builder()
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .emailVerified(true) // Auto-verified for directly created members
            .enabled(true)
            .accountLocked(false)
            .failedLoginAttempts(0)
            .build();

        user = userRepository.save(user);
        log.info("Created new user: {}", user.getEmail());

        // Create organization member
        OrganizationMemberEntity member = memberService.createMember(
            user.getId(),
            request.getOrganizationId(),
            request.getManagerId(),
            createdByUserId,
            request.getPosition(),
            request.getTitle()
        );

        // Assign role with scope
        UserRoleEntity userRole = UserRoleEntity.builder()
            .user(user)
            .role(role)
            .organization(organization)
            .client(request.getClientId() != null ?
                clientRepository.findById(request.getClientId()).orElse(null) : null)
            .build();

        userRoleRepository.save(userRole);

        log.info("Member created directly: {} in organization: {}",
            user.getEmail(), organization.getName());

        return UserDto.builder()
            .id(user.getId())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .emailVerified(user.getEmailVerified())
            .enabled(user.getEnabled())
            .build();
    }
}
