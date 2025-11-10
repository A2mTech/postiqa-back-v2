package fr.postiqa.gateway.auth.apikey;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;

/**
 * API Key Authentication Token for Spring Security.
 * Used for machine-to-machine authentication via API keys.
 */
@Getter
public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private final UserDetails principal;
    private final String apiKey;
    private final UUID userId;
    private final UUID apiKeyId;

    /**
     * Constructor for authenticated API key token
     */
    public ApiKeyAuthenticationToken(
        UserDetails principal,
        String apiKey,
        Collection<? extends GrantedAuthority> authorities,
        UUID userId,
        UUID apiKeyId
    ) {
        super(authorities);
        this.principal = principal;
        this.apiKey = apiKey;
        this.userId = userId;
        this.apiKeyId = apiKeyId;
        setAuthenticated(true);
    }

    /**
     * Constructor for unauthenticated API key token
     */
    public ApiKeyAuthenticationToken(String apiKey) {
        super(null);
        this.principal = null;
        this.apiKey = apiKey;
        this.userId = null;
        this.apiKeyId = null;
        setAuthenticated(false);
    }

    @Override
    public Object getCredentials() {
        return apiKey;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
}
