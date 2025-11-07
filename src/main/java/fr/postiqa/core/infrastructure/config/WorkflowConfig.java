package fr.postiqa.core.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for workflow execution infrastructure.
 * Configures async execution, thread pools, and workflow-specific beans.
 */
@Configuration
@EnableAsync
@Slf4j
public class WorkflowConfig {

    /**
     * Thread pool executor for async workflow step execution.
     * Configured with reasonable defaults for parallel step execution.
     */
    @Bean(name = "workflowExecutor")
    public Executor workflowExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core pool size: number of threads to keep alive
        executor.setCorePoolSize(10);

        // Max pool size: maximum number of threads
        executor.setMaxPoolSize(50);

        // Queue capacity: tasks to queue before rejecting
        executor.setQueueCapacity(100);

        // Thread name prefix for easy identification in logs
        executor.setThreadNamePrefix("workflow-exec-");

        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // Timeout for shutdown
        executor.setAwaitTerminationSeconds(60);

        // Rejection policy: caller runs the task if queue is full
        executor.setRejectedExecutionHandler(
            new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy()
        );

        executor.initialize();

        log.info("Initialized workflow executor with core pool size: {}, max pool size: {}",
            executor.getCorePoolSize(), executor.getMaxPoolSize());

        return executor;
    }
}
