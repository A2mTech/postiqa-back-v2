package fr.postiqa.gateway.auth.jwt;

import fr.postiqa.shared.exception.auth.InvalidTokenException;
import fr.postiqa.shared.exception.auth.TokenExpiredException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JWT Token Provider.
 * Handles JWT token generation, validation, and parsing.
 */
@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenValidityMs;
    private final long refreshTokenValidityMs;

    public JwtTokenProvider(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.access-token-validity-ms:900000}") long accessTokenValidityMs, // 15 minutes default
        @Value("${jwt.refresh-token-validity-ms:2592000000}") long refreshTokenValidityMs // 30 days default
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityMs = accessTokenValidityMs;
        this.refreshTokenValidityMs = refreshTokenValidityMs;
    }

    /**
     * Generate access token from authentication
     */
    public String generateAccessToken(Authentication authentication) {
        String email = authentication.getName();
        List<String> authorities = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());

        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(accessTokenValidityMs);

        return Jwts.builder()
            .subject(email)
            .claim("authorities", authorities)
            .claim("type", "access")
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .id(UUID.randomUUID().toString())
            .signWith(secretKey)
            .compact();
    }

    /**
     * Generate access token with custom claims
     */
    public String generateAccessToken(String email, List<String> authorities, Map<String, Object> customClaims) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(accessTokenValidityMs);

        JwtBuilder builder = Jwts.builder()
            .subject(email)
            .claim("authorities", authorities)
            .claim("type", "access")
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .id(UUID.randomUUID().toString());

        // Add custom claims (e.g., user_id, organization_id, client_id for scopes)
        if (customClaims != null) {
            customClaims.forEach(builder::claim);
        }

        return builder.signWith(secretKey).compact();
    }

    /**
     * Generate refresh token
     */
    public String generateRefreshToken(String email) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(refreshTokenValidityMs);

        return Jwts.builder()
            .subject(email)
            .claim("type", "refresh")
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .id(UUID.randomUUID().toString())
            .signWith(secretKey)
            .compact();
    }

    /**
     * Validate JWT token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
            throw new TokenExpiredException("Token has expired");
        } catch (SignatureException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
            throw new InvalidTokenException("Invalid token signature");
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            throw new InvalidTokenException("Malformed token");
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            throw new InvalidTokenException("Unsupported token");
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
            throw new InvalidTokenException("Token claims are empty");
        }
    }

    /**
     * Get email from JWT token
     */
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();

        return claims.getSubject();
    }

    /**
     * Get all claims from JWT token
     */
    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    /**
     * Get authorities from JWT token
     */
    @SuppressWarnings("unchecked")
    public List<String> getAuthoritiesFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return (List<String>) claims.get("authorities");
    }

    /**
     * Get expiration date from JWT token
     */
    public Date getExpirationFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration();
    }

    /**
     * Get token type (access or refresh)
     */
    public String getTokenType(String token) {
        Claims claims = getClaimsFromToken(token);
        return (String) claims.get("type");
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        Date expiration = getExpirationFromToken(token);
        return expiration.before(new Date());
    }

    /**
     * Get access token validity in seconds
     */
    public long getAccessTokenValiditySeconds() {
        return accessTokenValidityMs / 1000;
    }

    /**
     * Get refresh token validity in seconds
     */
    public long getRefreshTokenValiditySeconds() {
        return refreshTokenValidityMs / 1000;
    }
}
