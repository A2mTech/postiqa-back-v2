package fr.postiqa.core.domain.port;

import java.util.List;

/**
 * Port for audio/video transcription using Whisper API or similar models.
 * <p>
 * Transcribes audio from videos, podcasts, voice notes, and other audio sources.
 * Used in Phase 2C for video post analysis and weekly brief feature.
 */
public interface TranscriptionPort {

    /**
     * Transcribe audio from a URL (supports video and audio files).
     * Returns full transcription with timestamps.
     *
     * @param audioUrl URL of the audio/video file
     * @return Transcription result with segments
     */
    TranscriptionResult transcribeAudio(String audioUrl);

    /**
     * Transcribe audio with specified language hint.
     * Improves accuracy when language is known.
     *
     * @param audioUrl URL of the audio/video file
     * @param language ISO 639-1 language code (e.g., "en", "fr")
     * @return Transcription result with segments
     */
    TranscriptionResult transcribeAudio(String audioUrl, String language);

    /**
     * Transcribe audio with response format specification.
     * Supports different output formats.
     *
     * @param audioUrl URL of the audio/video file
     * @param language ISO 639-1 language code
     * @param responseFormat Format: "json", "text", "srt", "verbose_json", "vtt"
     * @return Transcription result
     */
    TranscriptionResult transcribeAudio(String audioUrl, String language, String responseFormat);

    /**
     * Get transcript text only (without timestamps).
     * Simplified method for when segments are not needed.
     *
     * @param audioUrl URL of the audio/video file
     * @return Plain text transcript
     */
    String getTranscriptText(String audioUrl);

    /**
     * Represents a complete transcription result with segments
     */
    record TranscriptionResult(
        String fullText,
        List<TranscriptionSegment> segments,
        String detectedLanguage,
        double duration
    ) {
        public TranscriptionResult {
            segments = segments != null ? List.copyOf(segments) : List.of();
        }

        public boolean hasSegments() {
            return segments != null && !segments.isEmpty();
        }

        public String getSegmentTextAtTime(double timestamp) {
            return segments.stream()
                .filter(seg -> seg.start() <= timestamp && timestamp <= seg.end())
                .findFirst()
                .map(TranscriptionSegment::text)
                .orElse("");
        }
    }

    /**
     * Represents a single segment of transcription with timing
     */
    record TranscriptionSegment(
        double start,
        double end,
        String text
    ) {
        public double duration() {
            return end - start;
        }
    }
}
