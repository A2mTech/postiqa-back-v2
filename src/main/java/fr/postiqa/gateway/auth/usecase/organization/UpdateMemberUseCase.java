package fr.postiqa.gateway.auth.usecase.organization;

import fr.postiqa.database.entity.OrganizationMemberEntity;
import fr.postiqa.database.repository.OrganizationMemberRepository;
import fr.postiqa.gateway.auth.service.OrganizationMemberService;
import fr.postiqa.shared.annotation.UseCase;
import fr.postiqa.shared.dto.auth.UpdateMemberRequest;
import fr.postiqa.shared.exception.auth.MemberNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Use case for updating a member's information.
 */
@UseCase(
    value = "UpdateMember",
    resourceType = "MEMBER",
    description = "Updates a member's information in the organization"
)
@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateMemberUseCase implements fr.postiqa.shared.usecase.UseCase<UpdateMemberRequest, Void> {

    private final OrganizationMemberService memberService;
    private final OrganizationMemberRepository memberRepository;

    @Transactional
    public Void execute(UpdateMemberRequest request) {
        // Get context from tenant holder
        UUID updatedByUserId = fr.postiqa.gateway.auth.authorization.TenantContextHolder.getUserId();
        UUID organizationId = fr.postiqa.gateway.auth.authorization.TenantContextHolder.getOrganizationId();
        UUID memberId = request.getMemberId();
        OrganizationMemberEntity member = memberRepository.findById(memberId)
            .orElseThrow(() -> new MemberNotFoundException("Member not found"));

        // Verify member belongs to the organization
        if (!member.getOrganization().getId().equals(organizationId)) {
            throw new MemberNotFoundException("Member not found in this organization");
        }

        Map<String, Object> changes = new HashMap<>();

        // Track changes for logging
        if (request.getPosition() != null && !request.getPosition().equals(member.getPosition())) {
            changes.put("position", Map.of("old", member.getPosition(), "new", request.getPosition()));
        }
        if (request.getTitle() != null && !request.getTitle().equals(member.getTitle())) {
            changes.put("title", Map.of("old", member.getTitle(), "new", request.getTitle()));
        }

        // Update manager if specified
        if (request.getManagerId() != null) {
            UUID currentManagerId = member.getManager() != null ? member.getManager().getId() : null;
            if (!request.getManagerId().equals(currentManagerId)) {
                memberService.setManager(memberId, request.getManagerId(), organizationId);
                changes.put("managerId", Map.of(
                    "old", currentManagerId != null ? currentManagerId.toString() : "null",
                    "new", request.getManagerId().toString()
                ));
            }
        }

        // Update status if specified
        OrganizationMemberEntity.MemberStatus status = null;
        if (request.getStatus() != null) {
            status = OrganizationMemberEntity.MemberStatus.valueOf(request.getStatus());
            if (status != member.getStatus()) {
                changes.put("status", Map.of("old", member.getStatus().name(), "new", status.name()));
            }
        }

        // Update member information
        memberService.updateMember(memberId, request.getPosition(), request.getTitle(), status);

        // Log activity if there were changes
        if (!changes.isEmpty()) {
            log.info("Updated member {}: {}", member.getUser().getEmail(), changes);
        }

        return null;
    }
}
