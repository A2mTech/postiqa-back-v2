package fr.postiqa.shared.annotation.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation to mark endpoints as publicly accessible (no authentication required).
 * Endpoints with this annotation will bypass Spring Security authentication.
 *
 * Usage: @PublicEndpoint
 *
 * Example: Login, Register, Password Reset endpoints
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PublicEndpoint {
}
