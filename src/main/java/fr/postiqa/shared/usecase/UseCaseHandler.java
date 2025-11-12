package fr.postiqa.shared.usecase;

import fr.postiqa.gateway.auth.authorization.TenantContextHolder;
import fr.postiqa.gateway.auth.service.ActivityLogService;
import fr.postiqa.shared.annotation.UseCase;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Unified handler for all use cases in the system.
 * Provides automatic logging, audit trail, performance monitoring, and error handling.
 *
 * <p>This aspect intercepts all use cases annotated with @UseCase and:
 * <ul>
 *   <li>Logs execution start/end with duration</li>
 *   <li>Creates audit trail entries</li>
 *   <li>Captures tenant context (user, organization, client)</li>
 *   <li>Extracts HTTP request information (IP, user agent)</li>
 *   <li>Tracks performance metrics</li>
 *   <li>Handles errors and exceptions</li>
 * </ul>
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class UseCaseHandler {

    private final ActivityLogService activityLogService;

    /**
     * Intercept all classes annotated with @UseCase
     */
    @Around("@within(fr.postiqa.shared.annotation.UseCase)")
    public Object handleUseCaseExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        // Get the @UseCase annotation from the target class
        UseCase useCaseAnnotation = AnnotationUtils.findAnnotation(
            joinPoint.getTarget().getClass(),
            UseCase.class
        );

        if (useCaseAnnotation == null) {
            // No annotation found, just proceed
            return joinPoint.proceed();
        }

        // Create execution context
        UseCaseExecutionContext context = createExecutionContext(joinPoint, useCaseAnnotation);

        try {
            // Extract tenant context
            extractTenantContext(context);

            // Extract HTTP request info
            extractHttpInfo(context);

            // Extract metadata from input
            extractInputMetadata(context, joinPoint);

            // Log execution start
            logExecutionStart(context);

            // Execute the use case
            Object result = joinPoint.proceed();

            // Extract resource ID from result if needed
            if (useCaseAnnotation.extractResourceId()) {
                extractResourceId(context, joinPoint.getArgs(), result);
            }

            // Mark success
            context.markSuccess();

            // Log execution end
            logExecutionEnd(context);

            // Create audit trail if enabled
            if (shouldLogActivity(useCaseAnnotation, context)) {
                createAuditTrail(context);
            }

            return result;

        } catch (Throwable error) {
            // Mark failure
            context.markFailure(error);

            // Log error
            logExecutionError(context);

            // Create audit trail for failed execution if enabled
            if (shouldLogActivity(useCaseAnnotation, context)) {
                try {
                    createAuditTrail(context);
                } catch (Exception auditError) {
                    log.error("Failed to create audit trail for failed use case: {}",
                        context.getUseCaseName(), auditError);
                }
            }

            // Re-throw the error
            throw error;
        }
    }

    /**
     * Create initial execution context
     */
    private UseCaseExecutionContext createExecutionContext(
        ProceedingJoinPoint joinPoint,
        UseCase annotation
    ) {
        String useCaseName = deriveUseCaseName(joinPoint, annotation);
        String resourceType = annotation.resourceType();
        String action = deriveActionName(useCaseName);

        return UseCaseExecutionContext.builder()
            .executionId(UUID.randomUUID())
            .useCaseName(useCaseName)
            .resourceType(resourceType.isEmpty() ? null : resourceType)
            .action(action)
            .startTime(Instant.now())
            .success(false)
            .build();
    }

    /**
     * Derive use case name from annotation or class name
     */
    private String deriveUseCaseName(ProceedingJoinPoint joinPoint, UseCase annotation) {
        if (!annotation.value().isEmpty()) {
            return annotation.value();
        }

        // Derive from class name: CreatePostUseCase -> CreatePost
        String className = joinPoint.getTarget().getClass().getSimpleName();
        return className.replace("UseCase", "");
    }

    /**
     * Derive action name from use case name
     * Example: CreatePost -> POST_CREATED
     */
    private String deriveActionName(String useCaseName) {
        // Convert camelCase to SNAKE_CASE
        String snakeCase = useCaseName
            .replaceAll("([a-z])([A-Z])", "$1_$2")
            .toUpperCase();

        // Handle common patterns
        if (snakeCase.startsWith("CREATE_")) {
            return snakeCase.replace("CREATE_", "") + "_CREATED";
        } else if (snakeCase.startsWith("UPDATE_")) {
            return snakeCase.replace("UPDATE_", "") + "_UPDATED";
        } else if (snakeCase.startsWith("DELETE_")) {
            return snakeCase.replace("DELETE_", "") + "_DELETED";
        } else if (snakeCase.startsWith("GET_") || snakeCase.startsWith("FETCH_")) {
            return snakeCase.replace("GET_", "").replace("FETCH_", "") + "_RETRIEVED";
        }

        return snakeCase;
    }

    /**
     * Extract tenant context from TenantContextHolder
     */
    private void extractTenantContext(UseCaseExecutionContext context) {
        try {
            UUID userId = TenantContextHolder.getUserId();
            UUID organizationId = TenantContextHolder.getOrganizationId();
            UUID clientId = TenantContextHolder.getClientId();

            context.setUserId(userId);
            context.setOrganizationId(organizationId);
            context.setClientId(clientId);

            if (userId != null) {
                context.addMetadata("userId", userId.toString());
            }
            if (organizationId != null) {
                context.addMetadata("organizationId", organizationId.toString());
            }
            if (clientId != null) {
                context.addMetadata("clientId", clientId.toString());
            }
        } catch (Exception e) {
            log.debug("Could not extract tenant context for use case: {}",
                context.getUseCaseName(), e);
        }
    }

    /**
     * Extract HTTP request information
     */
    private void extractHttpInfo(UseCaseExecutionContext context) {
        try {
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                context.setIpAddress(getClientIpAddress(request));
                context.setUserAgent(request.getHeader("User-Agent"));

                context.addMetadata("method", request.getMethod());
                context.addMetadata("uri", request.getRequestURI());
            }
        } catch (Exception e) {
            log.debug("Could not extract HTTP info for use case: {}",
                context.getUseCaseName(), e);
        }
    }

    /**
     * Extract metadata from use case input parameters
     */
    private void extractInputMetadata(UseCaseExecutionContext context, ProceedingJoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                for (int i = 0; i < args.length; i++) {
                    Object arg = args[i];
                    if (arg != null && !(arg instanceof UUID)) {
                        // Add simple representation of the argument
                        String argName = "input" + (args.length > 1 ? i : "");
                        context.addMetadata(argName, arg.getClass().getSimpleName());
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract input metadata for use case: {}",
                context.getUseCaseName(), e);
        }
    }

    /**
     * Extract resource ID from parameters or return value
     */
    private void extractResourceId(UseCaseExecutionContext context, Object[] args, Object result) {
        UUID resourceId = null;

        // Try to find UUID in parameters
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof UUID) {
                    resourceId = (UUID) arg;
                    break;
                }
                // Try to extract UUID from command objects using reflection
                if (arg != null) {
                    resourceId = extractUuidFromObject(arg);
                    if (resourceId != null) break;
                }
            }
        }

        // If not found in parameters, try result
        if (resourceId == null && result != null) {
            if (result instanceof UUID) {
                resourceId = (UUID) result;
            } else {
                // Try to extract from result object
                resourceId = extractUuidFromObject(result);
            }
        }

        if (resourceId != null) {
            context.setResourceId(resourceId);
            context.addMetadata("resourceId", resourceId.toString());
        }
    }

    /**
     * Extract UUID from object using reflection (looks for id, getId(), or any UUID field)
     */
    private UUID extractUuidFromObject(Object obj) {
        if (obj == null) return null;

        try {
            // Try getId() method
            Method getIdMethod = obj.getClass().getMethod("getId");
            Object id = getIdMethod.invoke(obj);
            if (id instanceof UUID) {
                return (UUID) id;
            }
        } catch (Exception e) {
            // Ignore, try fields
        }

        try {
            // Try to find any UUID field
            for (Field field : obj.getClass().getDeclaredFields()) {
                if (field.getType() == UUID.class) {
                    field.setAccessible(true);
                    Object value = field.get(obj);
                    if (value instanceof UUID) {
                        return (UUID) value;
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }

        return null;
    }

    /**
     * Check if activity should be logged
     */
    private boolean shouldLogActivity(UseCase annotation, UseCaseExecutionContext context) {
        return annotation.logActivity()
            && !annotation.resourceType().isEmpty()
            && context.getOrganizationId() != null
            && context.getUserId() != null;
    }

    /**
     * Log use case execution start
     */
    private void logExecutionStart(UseCaseExecutionContext context) {
        log.info("Executing use case [{}] - executionId={}, user={}, org={}",
            context.getUseCaseName(),
            context.getExecutionId(),
            context.getUserId(),
            context.getOrganizationId());
    }

    /**
     * Log use case execution end
     */
    private void logExecutionEnd(UseCaseExecutionContext context) {
        log.info("Completed use case [{}] in {}ms - executionId={}, resourceId={}",
            context.getUseCaseName(),
            context.getDurationMillis(),
            context.getExecutionId(),
            context.getResourceId());
    }

    /**
     * Log use case execution error
     */
    private void logExecutionError(UseCaseExecutionContext context) {
        log.error("Failed use case [{}] after {}ms - executionId={}, error={}: {}",
            context.getUseCaseName(),
            context.getDurationMillis(),
            context.getExecutionId(),
            context.getErrorType(),
            context.getErrorMessage(),
            context.getError());
    }

    /**
     * Create audit trail entry
     */
    private void createAuditTrail(UseCaseExecutionContext context) {
        try {
            Map<String, Object> metadata = new HashMap<>(context.getMetadata());
            metadata.put("executionId", context.getExecutionId().toString());
            metadata.put("durationMs", context.getDurationMillis());
            metadata.put("success", context.isSuccess());

            if (!context.isSuccess()) {
                metadata.put("errorType", context.getErrorType());
                metadata.put("errorMessage", context.getErrorMessage());
            }

            activityLogService.logActivity(
                context.getUserId(),
                context.getOrganizationId(),
                context.getClientId(),
                context.getAction(),
                context.getResourceType(),
                context.getResourceId(),
                context.getIpAddress(),
                context.getUserAgent(),
                metadata
            );

            log.debug("Audit trail created for use case [{}] - action={}",
                context.getUseCaseName(), context.getAction());

        } catch (Exception e) {
            log.error("Failed to create audit trail for use case: {}",
                context.getUseCaseName(), e);
        }
    }

    /**
     * Get client IP address from HTTP request, handling proxies
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
