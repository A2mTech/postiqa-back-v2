package fr.postiqa.features.postmanagement.domain.exception;

import fr.postiqa.features.postmanagement.domain.vo.ChannelId;

/**
 * Exception thrown when a channel is not found.
 */
public class ChannelNotFoundException extends DomainException {

    private final ChannelId channelId;

    public ChannelNotFoundException(ChannelId channelId) {
        super("Channel not found: " + channelId);
        this.channelId = channelId;
    }

    public ChannelNotFoundException(String channelId) {
        super("Channel not found: " + channelId);
        this.channelId = ChannelId.of(channelId);
    }

    public ChannelId getChannelId() {
        return channelId;
    }
}
