package fr.postiqa.shared.enums;

/**
 * Type of social media account.
 * Used to distinguish between business/professional accounts and personal accounts.
 */
public enum AccountType {
    BUSINESS("Business/Professional"),
    PERSONAL("Personal");

    private final String displayName;

    AccountType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
