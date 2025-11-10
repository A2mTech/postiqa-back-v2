package fr.postiqa.gateway.auth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2 Controller.
 * Provides information endpoints for OAuth2 authentication.
 * Note: Actual OAuth2 flow is handled by Spring Security's OAuth2 client.
 */
@RestController
@RequestMapping("/api/auth/oauth2")
@RequiredArgsConstructor
@Slf4j
public class OAuth2Controller {

    /**
     * Get OAuth2 authorization URL
     * GET /api/auth/oauth2/authorize/{provider}
     *
     * Returns information about where to redirect for OAuth2 login.
     * Actual authorization is handled by Spring Security at /oauth2/authorization/{provider}
     */
    @GetMapping("/authorize/{provider}")
    public ResponseEntity<Map<String, String>> getAuthorizationUrl(@PathVariable String provider) {
        log.debug("Getting OAuth2 authorization URL for provider: {}", provider);

        Map<String, String> response = new HashMap<>();
        response.put("provider", provider);
        response.put("authorizationUrl", "/oauth2/authorization/" + provider);
        response.put("description", "Redirect to this URL to initiate OAuth2 login with " + provider);

        return ResponseEntity.ok(response);
    }

    /**
     * OAuth2 providers information
     * GET /api/auth/oauth2/providers
     *
     * Returns list of available OAuth2 providers
     */
    @GetMapping("/providers")
    public ResponseEntity<Map<String, Object>> getProviders() {
        log.debug("Getting list of OAuth2 providers");

        Map<String, Object> response = new HashMap<>();
        response.put("providers", new String[]{"google", "github", "linkedin"});
        response.put("description", "Available OAuth2 providers. Use /api/auth/oauth2/authorize/{provider} to initiate login.");

        return ResponseEntity.ok(response);
    }
}
