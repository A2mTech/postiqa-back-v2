package fr.postiqa.gateway.auth.usecase;

import fr.postiqa.database.entity.RoleEntity;
import fr.postiqa.database.entity.UserEntity;
import fr.postiqa.database.entity.UserRoleEntity;
import fr.postiqa.database.repository.RoleRepository;
import fr.postiqa.database.repository.UserRepository;
import fr.postiqa.database.repository.UserRoleRepository;
import fr.postiqa.shared.dto.auth.RegisterRequest;
import fr.postiqa.shared.dto.auth.RegisterResponse;
import fr.postiqa.shared.dto.auth.UserDto;
import fr.postiqa.shared.exception.auth.UserAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Register Use Case.
 * Single responsibility: Register new user with default role.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RegisterUseCase {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SendVerificationEmailUseCase sendVerificationEmailUseCase;

    @Transactional
    public RegisterResponse execute(RegisterRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new UserAlreadyExistsException("User with email " + request.getEmail() + " already exists");
        }

        // Create user
        UserEntity user = UserEntity.builder()
            .email(request.getEmail().toLowerCase())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .emailVerified(false)
            .enabled(true)
            .accountLocked(false)
            .failedLoginAttempts(0)
            .build();

        user = userRepository.save(user);

        // Assign default role (BUSINESS_USER)
        assignDefaultRole(user);

        // Send verification email
        sendVerificationEmailUseCase.execute(user);

        log.info("User registered successfully: {}", user.getEmail());

        return buildRegisterResponse(user);
    }

    private void assignDefaultRole(UserEntity user) {
        RoleEntity defaultRole = roleRepository.findByName("BUSINESS_USER")
            .orElseThrow(() -> new RuntimeException("Default role BUSINESS_USER not found"));

        UserRoleEntity userRole = UserRoleEntity.builder()
            .user(user)
            .role(defaultRole)
            .build();

        userRoleRepository.save(userRole);
    }

    private RegisterResponse buildRegisterResponse(UserEntity user) {
        UserDto userDto = UserDto.builder()
            .id(user.getId())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .emailVerified(user.getEmailVerified())
            .enabled(user.getEnabled())
            .accountLocked(user.getAccountLocked())
            .createdAt(user.getCreatedAt())
            .build();

        return RegisterResponse.builder()
            .user(userDto)
            .message("Registration successful. Please verify your email.")
            .emailVerificationSent(true)
            .build();
    }
}
