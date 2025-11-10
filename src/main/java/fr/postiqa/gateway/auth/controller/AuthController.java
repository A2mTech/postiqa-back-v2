package fr.postiqa.gateway.auth.controller;

import fr.postiqa.gateway.auth.CustomUserDetails;
import fr.postiqa.gateway.auth.usecase.*;
import fr.postiqa.shared.dto.auth.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication Controller.
 * Provides REST endpoints for user authentication, registration, and token management.
 * Follows SOLID principles by delegating to specific use cases.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final RegisterUseCase registerUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final ForgotPasswordUseCase forgotPasswordUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final ValidateResetTokenUseCase validateResetTokenUseCase;
    private final VerifyEmailUseCase verifyEmailUseCase;
    private final ResendVerificationEmailUseCase resendVerificationEmailUseCase;

    /**
     * Login endpoint
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());
        LoginResponse response = loginUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Register endpoint
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration attempt for email: {}", request.getEmail());
        RegisterResponse response = registerUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Refresh token endpoint
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh attempt");
        RefreshTokenResponse response = refreshTokenUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Logout endpoint
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("Logout attempt for user: {}", userDetails.getEmail());
        logoutUseCase.execute(userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Get current user endpoint
     * GET /api/auth/me
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UserDto user = UserDto.builder()
            .id(userDetails.getId())
            .email(userDetails.getEmail())
            .firstName(userDetails.getFirstName())
            .lastName(userDetails.getLastName())
            .emailVerified(userDetails.isEmailVerified())
            .enabled(userDetails.isEnabled())
            .accountLocked(!userDetails.isAccountNonLocked())
            .build();

        return ResponseEntity.ok(user);
    }

    /**
     * Forgot password endpoint
     * POST /api/auth/forgot-password
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Password reset requested for email: {}", request.getEmail());
        forgotPasswordUseCase.execute(request);
        return ResponseEntity.ok(Map.of(
            "message", "If an account exists with this email, a password reset link has been sent."
        ));
    }

    /**
     * Reset password endpoint
     * POST /api/auth/reset-password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Password reset attempt with token");
        resetPasswordUseCase.execute(request);
        return ResponseEntity.ok(Map.of(
            "message", "Password has been reset successfully. You can now log in with your new password."
        ));
    }

    /**
     * Validate reset token endpoint
     * GET /api/auth/validate-reset-token/{token}
     */
    @GetMapping("/validate-reset-token/{token}")
    public ResponseEntity<Map<String, Boolean>> validateResetToken(@PathVariable String token) {
        log.info("Validating password reset token");
        boolean isValid = validateResetTokenUseCase.execute(token);
        return ResponseEntity.ok(Map.of("valid", isValid));
    }

    /**
     * Verify email endpoint
     * GET /api/auth/verify-email/{token}
     */
    @GetMapping("/verify-email/{token}")
    public ResponseEntity<Map<String, String>> verifyEmail(@PathVariable String token) {
        log.info("Email verification attempt with token");
        verifyEmailUseCase.execute(token);
        return ResponseEntity.ok(Map.of(
            "message", "Email verified successfully. You can now log in."
        ));
    }

    /**
     * Resend verification email endpoint
     * POST /api/auth/resend-verification
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        log.info("Resend verification email requested for: {}", request.getEmail());
        resendVerificationEmailUseCase.execute(request);
        return ResponseEntity.ok(Map.of(
            "message", "If your email is not verified, a new verification link has been sent."
        ));
    }
}
