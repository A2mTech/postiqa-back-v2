package fr.postiqa.features.postmanagement.domain.exception;

import fr.postiqa.features.postmanagement.domain.vo.ChannelId;

/**
 * Exception thrown when an operation cannot be performed due to channel state.
 */
public class InvalidChannelStateException extends DomainException {

    private final ChannelId channelId;
    private final String operation;

    public InvalidChannelStateException(ChannelId channelId, String operation) {
        super(String.format("Cannot %s channel %s in current state", operation, channelId));
        this.channelId = channelId;
        this.operation = operation;
    }

    public InvalidChannelStateException(ChannelId channelId, String operation, String reason) {
        super(String.format("Cannot %s channel %s: %s", operation, channelId, reason));
        this.channelId = channelId;
        this.operation = operation;
    }

    public ChannelId getChannelId() {
        return channelId;
    }

    public String getOperation() {
        return operation;
    }
}
