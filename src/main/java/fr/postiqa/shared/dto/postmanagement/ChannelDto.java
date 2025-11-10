package fr.postiqa.shared.dto.postmanagement;

import fr.postiqa.shared.enums.AccountType;
import fr.postiqa.shared.enums.SocialPlatform;

import java.time.Instant;

/**
 * Response DTO for a channel (social account).
 */
public record ChannelDto(
    String id,
    SocialPlatform platform,
    AccountType accountType,
    String accountName,
    String accountHandle,
    String avatarUrl,
    boolean active,
    Instant createdAt
) {}
