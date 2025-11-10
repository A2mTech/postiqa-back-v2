package fr.postiqa.features.postmanagement.domain.exception;

import fr.postiqa.features.postmanagement.domain.vo.MediaId;

/**
 * Exception thrown when a media file is not found.
 */
public class MediaNotFoundException extends DomainException {

    private final MediaId mediaId;

    public MediaNotFoundException(MediaId mediaId) {
        super("Media not found: " + mediaId);
        this.mediaId = mediaId;
    }

    public MediaNotFoundException(String mediaId) {
        super("Media not found: " + mediaId);
        this.mediaId = MediaId.of(mediaId);
    }

    public MediaId getMediaId() {
        return mediaId;
    }
}
