package fr.postiqa.features.postmanagement.domain.vo;

import fr.postiqa.shared.enums.AccountType;
import fr.postiqa.shared.enums.SocialPlatform;

/**
 * Value object representing a social media channel's profile information.
 */
public record ChannelProfile(
    SocialPlatform platform,
    AccountType accountType,
    String accountName,
    String accountHandle,
    String profileUrl,
    String avatarUrl
) {

    public ChannelProfile {
        if (platform == null) {
            throw new IllegalArgumentException("Platform cannot be null");
        }
        if (accountType == null) {
            throw new IllegalArgumentException("Account type cannot be null");
        }
        if (accountName == null || accountName.isBlank()) {
            throw new IllegalArgumentException("Account name cannot be null or blank");
        }
    }

    /**
     * Create a channel profile
     */
    public static ChannelProfile create(
        SocialPlatform platform,
        AccountType accountType,
        String accountName,
        String accountHandle,
        String profileUrl,
        String avatarUrl
    ) {
        return new ChannelProfile(platform, accountType, accountName, accountHandle, profileUrl, avatarUrl);
    }

    /**
     * Check if the profile has a handle
     */
    public boolean hasHandle() {
        return accountHandle != null && !accountHandle.isBlank();
    }

    /**
     * Check if the profile has an avatar
     */
    public boolean hasAvatar() {
        return avatarUrl != null && !avatarUrl.isBlank();
    }

    /**
     * Check if this is a business/professional account
     */
    public boolean isBusiness() {
        return accountType == AccountType.BUSINESS;
    }

    /**
     * Check if this is a personal account
     */
    public boolean isPersonal() {
        return accountType == AccountType.PERSONAL;
    }
}
