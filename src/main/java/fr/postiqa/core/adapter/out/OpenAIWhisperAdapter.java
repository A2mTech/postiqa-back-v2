package fr.postiqa.core.adapter.out;

import fr.postiqa.core.domain.port.TranscriptionPort;
import fr.postiqa.core.infrastructure.client.WhisperClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * OpenAI Whisper implementation of TranscriptionPort.
 * <p>
 * Provides audio/video transcription using Whisper API.
 * Supports multiple languages and output formats.
 */
@Component
public class OpenAIWhisperAdapter implements TranscriptionPort {

    private static final Logger log = LoggerFactory.getLogger(OpenAIWhisperAdapter.class);

    private final WhisperClient whisperClient;

    public OpenAIWhisperAdapter(WhisperClient whisperClient) {
        this.whisperClient = whisperClient;
    }

    @Override
    public TranscriptionResult transcribeAudio(String audioUrl) {
        log.info("Transcribing audio/video: {}", audioUrl);
        return whisperClient.transcribe(audioUrl);
    }

    @Override
    public TranscriptionResult transcribeAudio(String audioUrl, String language) {
        log.info("Transcribing audio/video with language hint '{}': {}", language, audioUrl);
        return whisperClient.transcribe(audioUrl, language);
    }

    @Override
    public TranscriptionResult transcribeAudio(String audioUrl, String language, String responseFormat) {
        log.info("Transcribing audio/video (format: {}): {}", responseFormat, audioUrl);
        return whisperClient.transcribe(audioUrl, language, responseFormat);
    }

    @Override
    public String getTranscriptText(String audioUrl) {
        log.info("Getting transcript text only: {}", audioUrl);
        TranscriptionResult result = whisperClient.transcribe(audioUrl);
        return result.fullText();
    }
}
