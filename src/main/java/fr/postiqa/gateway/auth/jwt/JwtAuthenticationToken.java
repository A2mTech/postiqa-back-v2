package fr.postiqa.gateway.auth.jwt;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * JWT Authentication Token for Spring Security.
 * Extends AbstractAuthenticationToken to integrate with Spring Security.
 */
@Getter
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final UserDetails principal;
    private final String token;
    private final UUID userId;
    private final UUID organizationId;
    private final UUID clientId;
    private final Map<String, Object> claims;

    /**
     * Constructor for authenticated JWT token
     */
    public JwtAuthenticationToken(
        UserDetails principal,
        String token,
        Collection<? extends GrantedAuthority> authorities,
        UUID userId,
        UUID organizationId,
        UUID clientId,
        Map<String, Object> claims
    ) {
        super(authorities);
        this.principal = principal;
        this.token = token;
        this.userId = userId;
        this.organizationId = organizationId;
        this.clientId = clientId;
        this.claims = claims;
        setAuthenticated(true);
    }

    /**
     * Constructor for unauthenticated JWT token
     */
    public JwtAuthenticationToken(String token) {
        super(null);
        this.principal = null;
        this.token = token;
        this.userId = null;
        this.organizationId = null;
        this.clientId = null;
        this.claims = null;
        setAuthenticated(false);
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
}
