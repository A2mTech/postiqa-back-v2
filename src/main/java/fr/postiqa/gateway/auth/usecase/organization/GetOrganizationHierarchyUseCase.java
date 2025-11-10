package fr.postiqa.gateway.auth.usecase.organization;

import fr.postiqa.database.entity.OrganizationMemberEntity;
import fr.postiqa.database.repository.UserRoleRepository;
import fr.postiqa.gateway.auth.service.OrganizationMemberService;
import fr.postiqa.shared.dto.auth.OrganizationHierarchyDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Use case for getting organization hierarchy tree.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GetOrganizationHierarchyUseCase {

    private final OrganizationMemberService memberService;
    private final UserRoleRepository userRoleRepository;

    @Transactional(readOnly = true)
    public List<OrganizationHierarchyDto> execute(UUID organizationId) {
        List<OrganizationMemberEntity> allMembers = memberService.getActiveMembers(organizationId);

        // Build hierarchy: find top-level members (no manager)
        List<OrganizationMemberEntity> topLevel = allMembers.stream()
            .filter(m -> m.getManager() == null)
            .collect(Collectors.toList());

        // Build tree recursively
        return topLevel.stream()
            .map(member -> buildHierarchyNode(member, allMembers, organizationId))
            .collect(Collectors.toList());
    }

    private OrganizationHierarchyDto buildHierarchyNode(
        OrganizationMemberEntity member,
        List<OrganizationMemberEntity> allMembers,
        UUID organizationId
    ) {
        // Get direct reports
        List<OrganizationMemberEntity> directReports = allMembers.stream()
            .filter(m -> m.getManager() != null && m.getManager().getId().equals(member.getUser().getId()))
            .collect(Collectors.toList());

        // Get roles for this user
        var roles = userRoleRepository.findByUserIdAndOrganizationId(
            member.getUser().getId(),
            organizationId
        );

        List<String> roleNames = roles.stream()
            .map(ur -> ur.getRole().getName())
            .collect(Collectors.toList());

        // Build children recursively
        List<OrganizationHierarchyDto> children = directReports.stream()
            .map(report -> buildHierarchyNode(report, allMembers, organizationId))
            .collect(Collectors.toList());

        return OrganizationHierarchyDto.builder()
            .userId(member.getUser().getId())
            .email(member.getUser().getEmail())
            .firstName(member.getUser().getFirstName())
            .lastName(member.getUser().getLastName())
            .position(member.getPosition())
            .title(member.getTitle())
            .roles(roleNames)
            .directReportsCount(directReports.size())
            .directReports(children)
            .build();
    }
}
