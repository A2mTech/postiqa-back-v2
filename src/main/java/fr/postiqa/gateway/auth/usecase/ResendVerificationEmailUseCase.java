package fr.postiqa.gateway.auth.usecase;

import fr.postiqa.database.entity.EmailVerificationTokenEntity;
import fr.postiqa.database.entity.UserEntity;
import fr.postiqa.database.repository.EmailVerificationTokenRepository;
import fr.postiqa.database.repository.UserRepository;
import fr.postiqa.gateway.auth.email.EmailService;
import fr.postiqa.shared.dto.auth.ResendVerificationRequest;
import fr.postiqa.shared.exception.auth.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Resend Verification Email Use Case.
 * Single responsibility: Generate new verification token and resend verification email.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ResendVerificationEmailUseCase {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailService emailService;

    @Value("${mail.verification.url}")
    private String verificationBaseUrl;

    @Value("${mail.verification.token-validity-hours}")
    private int tokenValidityHours;

    @Transactional
    public void execute(ResendVerificationRequest request) {
        // Find user by email
        UserEntity user = userRepository.findByEmailIgnoreCase(request.getEmail())
            .orElseThrow(() -> new UserNotFoundException("User not found with email: " + request.getEmail()));

        // Check if email is already verified
        if (user.getEmailVerified()) {
            log.info("Email already verified for user: {}", user.getEmail());
            return;
        }

        // Invalidate all previous verification tokens for this user
        tokenRepository.invalidateAllByUserId(user.getId(), Instant.now());

        // Generate new verification token
        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(tokenValidityHours, ChronoUnit.HOURS);

        EmailVerificationTokenEntity verificationToken = EmailVerificationTokenEntity.builder()
            .user(user)
            .token(token)
            .expiresAt(expiresAt)
            .used(false)
            .build();

        tokenRepository.save(verificationToken);

        // Send verification email
        String verificationUrl = verificationBaseUrl + "/" + token;
        emailService.sendEmailVerification(user.getEmail(), user.getFirstName(), verificationUrl);

        log.info("Email verification resent for user: {}", user.getEmail());
    }
}
