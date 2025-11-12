package fr.postiqa.gateway.auth.usecase.organization;

import fr.postiqa.database.entity.*;
import fr.postiqa.database.repository.UserRepository;
import fr.postiqa.database.repository.UserRoleRepository;
import fr.postiqa.gateway.auth.service.InvitationService;
import fr.postiqa.gateway.auth.service.OrganizationMemberService;
import fr.postiqa.shared.annotation.UseCase;
import fr.postiqa.shared.dto.auth.AcceptInvitationRequest;
import fr.postiqa.shared.dto.auth.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Use case for accepting an organization invitation.
 */
@UseCase(
    value = "AcceptInvitation",
    resourceType = "MEMBER",
    description = "Accepts an organization invitation and creates member"
)
@Service
@RequiredArgsConstructor
@Slf4j
public class AcceptInvitationUseCase implements fr.postiqa.shared.usecase.UseCase<AcceptInvitationRequest, UserDto> {

    private final InvitationService invitationService;
    private final OrganizationMemberService memberService;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserDto execute(AcceptInvitationRequest request) {
        // Validate invitation
        OrganizationInvitationEntity invitation = invitationService.validateInvitation(request.getToken());

        // Check if user already exists
        Optional<UserEntity> existingUser = userRepository.findByEmailIgnoreCase(invitation.getEmail());

        UserEntity user;
        boolean isNewUser = false;

        if (existingUser.isPresent()) {
            user = existingUser.get();
        } else {
            // Create new user
            if (request.getPassword() == null || request.getPassword().isEmpty()) {
                throw new IllegalArgumentException("Password is required for new users");
            }

            user = UserEntity.builder()
                .email(invitation.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .emailVerified(true) // Auto-verify since they came from invitation
                .enabled(true)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .build();

            user = userRepository.save(user);
            isNewUser = true;
            log.info("Created new user: {}", user.getEmail());
        }

        // Create organization member
        OrganizationMemberEntity member = memberService.createMember(
            user.getId(),
            invitation.getOrganization().getId(),
            null, // Manager can be set later
            invitation.getInvitedBy().getId(),
            null, // Position
            null  // Title
        );

        // Assign role with scope
        UserRoleEntity userRole = UserRoleEntity.builder()
            .user(user)
            .role(invitation.getRole())
            .organization(invitation.getOrganization())
            .client(invitation.getClient())
            .build();

        userRoleRepository.save(userRole);

        // Mark invitation as accepted
        invitationService.markAsAccepted(invitation.getId(), user.getId());

        log.info("Invitation accepted by user: {} for organization: {}",
            user.getEmail(), invitation.getOrganization().getName());

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
