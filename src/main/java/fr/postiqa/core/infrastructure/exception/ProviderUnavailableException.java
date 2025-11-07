package fr.postiqa.core.infrastructure.exception;

/**
 * Exception thrown when a scraping provider (Apify, etc.) is unavailable
 */
public class ProviderUnavailableException extends ScrapingException {

    private final String providerName;

    public ProviderUnavailableException(String providerName, String message) {
        super(String.format("Provider '%s' is unavailable: %s", providerName, message));
        this.providerName = providerName;
    }

    public ProviderUnavailableException(String providerName, String message, Throwable cause) {
        super(String.format("Provider '%s' is unavailable: %s", providerName, message), cause);
        this.providerName = providerName;
    }

    public String getProviderName() {
        return providerName;
    }
}
