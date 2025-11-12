package fr.postiqa.shared.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Configuration for enabling AspectJ AOP support.
 * This enables:
 * <ul>
 *   <li>UseCaseHandler - Unified use case logging and audit trail</li>
 *   <li>ActivityLoggingAspect - Activity logging for @LogActivity annotation</li>
 * </ul>
 */
@Configuration
@EnableAspectJAutoProxy
@Slf4j
public class AspectConfiguration {

    public AspectConfiguration() {
        log.info("AspectJ AOP support enabled - UseCaseHandler and ActivityLoggingAspect are active");
    }
}
