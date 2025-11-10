package fr.postiqa.features.postmanagement.adapter.out.event;

import fr.postiqa.features.postmanagement.domain.port.PostEventPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Spring event adapter implementing PostEventPort.
 * Uses Spring's ApplicationEventPublisher for inter-module communication via Spring Modulith.
 */
@Component
public class SpringPostEventAdapter implements PostEventPort {

    private final ApplicationEventPublisher eventPublisher;

    public SpringPostEventAdapter(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void publishPostCreated(PostCreatedEvent event) {
        eventPublisher.publishEvent(event);
    }

    @Override
    public void publishPostScheduled(PostScheduledEvent event) {
        eventPublisher.publishEvent(event);
    }

    @Override
    public void publishPostScheduleCancelled(PostScheduleCancelledEvent event) {
        eventPublisher.publishEvent(event);
    }

    @Override
    public void publishPostPublished(PostPublishedEvent event) {
        eventPublisher.publishEvent(event);
    }

    @Override
    public void publishPostPublishFailed(PostPublishFailedEvent event) {
        eventPublisher.publishEvent(event);
    }

    @Override
    public void publishPostDeleted(PostDeletedEvent event) {
        eventPublisher.publishEvent(event);
    }
}
