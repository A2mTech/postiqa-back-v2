package fr.postiqa.gateway.auth.controller;

import fr.postiqa.gateway.auth.usecase.organization.AcceptInvitationUseCase;
import fr.postiqa.gateway.auth.usecase.organization.ValidateInvitationUseCase;
import fr.postiqa.shared.annotation.security.PublicEndpoint;
import fr.postiqa.shared.dto.auth.AcceptInvitationRequest;
import fr.postiqa.shared.dto.auth.InvitationDto;
import fr.postiqa.shared.dto.auth.UserDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public controller for handling organization invitations.
 * These endpoints are accessible without authentication.
 */
@RestController
@RequestMapping("/api/auth/invitations")
@RequiredArgsConstructor
@Slf4j
public class OrganizationInvitationController {

    private final AcceptInvitationUseCase acceptInvitationUseCase;
    private final ValidateInvitationUseCase validateInvitationUseCase;

    /**
     * Accept an organization invitation.
     * This endpoint is public as the user may not exist yet.
     *
     * POST /api/auth/invitations/accept
     */
    @PostMapping("/accept")
    @PublicEndpoint
    public ResponseEntity<UserDto> acceptInvitation(
        @Valid @RequestBody AcceptInvitationRequest request
    ) {
        log.info("Accepting invitation with token");

        UserDto user = acceptInvitationUseCase.execute(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /**
     * Validate an invitation token (check if valid/expired).
     *
     * GET /api/auth/invitations/validate?token=xxx
     */
    @GetMapping("/validate")
    @PublicEndpoint
    public ResponseEntity<InvitationDto> validateInvitation(
        @RequestParam String token
    ) {
        log.info("Validating invitation token");

        InvitationDto invitation = validateInvitationUseCase.execute(token);

        return ResponseEntity.ok(invitation);
    }
}

