package fr.postiqa.core.usecase;

import fr.postiqa.core.domain.workflow.model.WorkflowContext;
import fr.postiqa.core.domain.workflow.model.WorkflowDefinition;
import fr.postiqa.core.infrastructure.workflow.ultradeep.UltraDeepAnalysisWorkflowFactory;
import fr.postiqa.core.usecase.workflow.StartWorkflowUseCase;
import fr.postiqa.database.entity.ClientEntity;
import fr.postiqa.database.entity.OrganizationEntity;
import fr.postiqa.database.entity.UserProfileAnalysisEntity;
import fr.postiqa.database.repository.ClientRepository;
import fr.postiqa.database.repository.OrganizationRepository;
import fr.postiqa.database.repository.UserProfileAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Use case for starting an ultra-deep profile analysis.
 * Creates a UserProfileAnalysisEntity, builds the workflow, and starts execution.
 */
@fr.postiqa.shared.annotation.UseCase(
    value = "StartUltraDeepAnalysis",
    resourceType = "USER_PROFILE_ANALYSIS",
    description = "Initiates ultra-deep profile analysis workflow"
)
@RequiredArgsConstructor
@Slf4j
public class StartUltraDeepAnalysisUseCase {

    private final StartWorkflowUseCase startWorkflowUseCase;
    private final UserProfileAnalysisRepository userProfileAnalysisRepository;
    private final OrganizationRepository organizationRepository;
    private final ClientRepository clientRepository;
    private final UltraDeepAnalysisWorkflowFactory workflowFactory;

    /**
     * Execute the ultra-deep analysis workflow
     *
     * @param command Command containing analysis parameters
     * @return Workflow instance ID
     */
    public String execute(StartUltraDeepAnalysisCommand command) {
        log.info("Starting ultra-deep analysis for organization {} and client {}",
            command.organizationId(), command.clientId());

        // 1. Validate and fetch organization
        OrganizationEntity organization = organizationRepository.findById(command.organizationId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Organization not found: " + command.organizationId()));

        // 2. Validate and fetch client (optional for BUSINESS organizations)
        ClientEntity client = null;
        if (command.clientId() != null) {
            client = clientRepository.findById(command.clientId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Client not found: " + command.clientId()));

            // Validate that client belongs to this organization
            if (!client.getAgency().getId().equals(command.organizationId())) {
                throw new IllegalArgumentException(
                    "Client does not belong to this organization");
            }
        }

        // 3. Create UserProfileAnalysisEntity
        UserProfileAnalysisEntity analysis = UserProfileAnalysisEntity.builder()
            .organization(organization)
            .client(client)
            .platforms(command.platforms())
            .siteUrl(command.websiteUrl())
            .status("PENDING")
            .userId("") // Will be populated during workflow
            .workflowInstanceId("") // Temporary, will be updated
            .build();

        // Save to get ID
        analysis = userProfileAnalysisRepository.save(analysis);
        log.info("Created analysis entity with ID: {}", analysis.getId());

        // 4. Build workflow definition with all 24 steps
        WorkflowDefinition workflow = workflowFactory.createWorkflow();

        // 5. Create initial context with all input data
        WorkflowContext context = WorkflowContext.empty()
            .put("user_profile_analysis_id", analysis.getId())
            .put("organization_id", command.organizationId())
            .put("client_id", command.clientId())
            .put("platforms", command.platforms())
            .put("website_url", command.websiteUrl())
            .put("linkedin_profile_url", command.linkedinProfileUrl())
            .put("twitter_profile_url", command.twitterProfileUrl())
            .put("instagram_profile_url", command.instagramProfileUrl())
            .put("youtube_profile_url", command.youtubeProfileUrl())
            .put("tiktok_profile_url", command.tiktokProfileUrl());

        // 6. Start workflow execution asynchronously
        String workflowInstanceId = startWorkflowUseCase.startAndReturnId(workflow, context);
        log.info("Started workflow instance: {}", workflowInstanceId);

        // 7. Update entity with workflow instance ID and status
        analysis.setWorkflowInstanceId(workflowInstanceId);
        analysis.setStatus("RUNNING");
        analysis.setStartedAt(Instant.now());
        userProfileAnalysisRepository.save(analysis);

        log.info("Ultra-deep analysis initiated successfully. Analysis ID: {}, Workflow ID: {}",
            analysis.getId(), workflowInstanceId);
        return workflowInstanceId;
    }

    /**
     * Command record for starting ultra-deep analysis
     */
    public record StartUltraDeepAnalysisCommand(
        UUID organizationId,
        UUID clientId,
        List<String> platforms,
        String websiteUrl,
        String linkedinProfileUrl,
        String twitterProfileUrl,
        String instagramProfileUrl,
        String youtubeProfileUrl,
        String tiktokProfileUrl
    ) {}
}
