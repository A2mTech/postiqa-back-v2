package fr.postiqa.gateway.auth.aspect;

import fr.postiqa.gateway.auth.authorization.TenantContextHolder;
import fr.postiqa.gateway.auth.service.ActivityLogService;
import fr.postiqa.shared.annotation.LogActivity;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Aspect for automatic activity logging.
 * Intercepts methods annotated with @LogActivity and logs them automatically.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityLoggingAspect {

    private final ActivityLogService activityLogService;

    @Around("@annotation(fr.postiqa.shared.annotation.LogActivity)")
    public Object logActivity(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        LogActivity annotation = method.getAnnotation(LogActivity.class);

        // Execute the method
        Object result = joinPoint.proceed();

        try {
            // Extract context
            UUID userId = TenantContextHolder.getUserId();
            UUID organizationId = TenantContextHolder.getOrganizationId();
            UUID clientId = TenantContextHolder.getClientId();

            // If context is not available, try to extract from method parameters
            if (userId == null || organizationId == null) {
                log.warn("Tenant context not available for activity logging on method: {}",
                    method.getName());
                return result;
            }

            // Determine action name
            String action = annotation.value().isEmpty() ?
                deriveActionFromMethodName(method.getName()) :
                annotation.value();

            // Extract resource ID
            UUID resourceId = null;
            if (annotation.extractResourceId()) {
                resourceId = extractResourceId(joinPoint, result);
            }

            // Extract HTTP request info
            String ipAddress = null;
            String userAgent = null;

            try {
                ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    ipAddress = getClientIpAddress(request);
                    userAgent = request.getHeader("User-Agent");
                }
            } catch (Exception e) {
                log.debug("Could not extract HTTP request info for activity logging", e);
            }

            // Build metadata
            Map<String, Object> metadata = buildMetadata(joinPoint, result);

            // Log the activity
            activityLogService.logActivity(
                userId,
                organizationId,
                clientId,
                action,
                annotation.resourceType(),
                resourceId,
                ipAddress,
                userAgent,
                metadata
            );

            log.debug("Activity logged: {} - {} for user {}", action, annotation.resourceType(), userId);

        } catch (Exception e) {
            log.error("Failed to log activity for method: {}", method.getName(), e);
            // Don't fail the original method execution
        }

        return result;
    }

    /**
     * Derive action name from method name.
     * Example: "inviteMember" -> "MEMBER_INVITED"
     */
    private String deriveActionFromMethodName(String methodName) {
        // Remove "execute" prefix if present
        if (methodName.startsWith("execute")) {
            methodName = methodName.substring(7);
        }

        // Convert camelCase to SNAKE_CASE
        return methodName
            .replaceAll("([a-z])([A-Z])", "$1_$2")
            .toUpperCase();
    }

    /**
     * Extract resource ID from method parameters or return value.
     */
    private UUID extractResourceId(ProceedingJoinPoint joinPoint, Object result) {
        // Try to find UUID in parameters
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof UUID) {
                return (UUID) arg;
            }
        }

        // Try to extract from result if it has an getId() method
        if (result != null) {
            try {
                Method getIdMethod = result.getClass().getMethod("getId");
                Object id = getIdMethod.invoke(result);
                if (id instanceof UUID) {
                    return (UUID) id;
                }
            } catch (Exception e) {
                // Ignore, ID not found in result
            }
        }

        return null;
    }

    /**
     * Build metadata from method parameters.
     */
    private Map<String, Object> buildMetadata(ProceedingJoinPoint joinPoint, Object result) {
        Map<String, Object> metadata = new HashMap<>();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        // Add relevant parameters (skip UUIDs as they're in separate fields)
        for (int i = 0; i < parameterNames.length && i < args.length; i++) {
            if (!(args[i] instanceof UUID)) {
                // Convert objects to String to avoid serialization issues
                metadata.put(parameterNames[i], String.valueOf(args[i]));
            }
        }

        metadata.put("method", signature.getMethod().getName());

        return metadata;
    }

    /**
     * Get client IP address from HTTP request, handling proxies.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For can contain multiple IPs, take the first
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }
}
