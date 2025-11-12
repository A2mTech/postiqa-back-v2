package fr.postiqa.core.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.postiqa.core.domain.port.TranscriptionPort;
import fr.postiqa.core.infrastructure.exception.ProviderUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * HTTP client for OpenAI Whisper API.
 * <p>
 * Handles audio/video transcription using Whisper.
 * Supports multiple languages and output formats.
 */
@Component
public class WhisperClient {

    private static final Logger log = LoggerFactory.getLogger(WhisperClient.class);
    private static final String WHISPER_API_URL = "https://api.openai.com/v1/audio/transcriptions";
    private static final String WHISPER_MODEL = "whisper-1";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String openAiApiKey;

    public WhisperClient(
        @Value("${openai.api-key}") String openAiApiKey,
        ObjectMapper objectMapper
    ) {
        this.openAiApiKey = openAiApiKey;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
            .baseUrl(WHISPER_API_URL)
            .defaultHeader("Authorization", "Bearer " + openAiApiKey)
            .build();
    }

    /**
     * Transcribe audio with default settings (verbose JSON format with timestamps)
     */
    public TranscriptionPort.TranscriptionResult transcribe(String audioUrl) {
        return transcribe(audioUrl, null, "verbose_json");
    }

    /**
     * Transcribe audio with language hint
     */
    public TranscriptionPort.TranscriptionResult transcribe(String audioUrl, String language) {
        return transcribe(audioUrl, language, "verbose_json");
    }

    /**
     * Transcribe audio with full options
     *
     * @param audioUrl       URL of the audio/video file
     * @param language       ISO 639-1 language code (optional, null for auto-detect)
     * @param responseFormat "json", "text", "srt", "verbose_json", "vtt"
     * @return Transcription result with segments
     */
    public TranscriptionPort.TranscriptionResult transcribe(
        String audioUrl,
        String language,
        String responseFormat
    ) {
        log.info("Transcribing audio from URL: {} (language: {}, format: {})",
            audioUrl, language != null ? language : "auto", responseFormat);

        try {
            // Download audio file from URL
            Resource audioResource = downloadAudioResource(audioUrl);

            // Build multipart request
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", audioResource);
            builder.part("model", WHISPER_MODEL);
            builder.part("response_format", responseFormat != null ? responseFormat : "verbose_json");
            if (language != null && !language.isBlank()) {
                builder.part("language", language);
            }
            builder.part("timestamp_granularities[]", "segment");

            // Make API call
            String response = restClient.post()
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(builder.build())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, resp) -> {
                    throw new ProviderUnavailableException("Whisper",
                        "Client error: " + resp.getStatusCode());
                })
                .onStatus(HttpStatusCode::is5xxServerError, (request, resp) -> {
                    throw new ProviderUnavailableException("Whisper",
                        "Server error: " + resp.getStatusCode());
                })
                .body(String.class);

            return parseTranscriptionResponse(response, responseFormat);

        } catch (IOException e) {
            log.error("Failed to download or process audio file: {}", audioUrl, e);
            throw new ProviderUnavailableException("Whisper", "Failed to process audio file", e);
        } catch (Exception e) {
            log.error("Failed to transcribe audio: {}", audioUrl, e);
            throw new ProviderUnavailableException("Whisper", "Transcription failed", e);
        }
    }

    /**
     * Download audio file as a Spring Resource
     */
    private Resource downloadAudioResource(String audioUrl) throws IOException {
        log.debug("Downloading audio from: {}", audioUrl);
        URL url = new URL(audioUrl);
        return new UrlResource(url);
    }

    /**
     * Parse Whisper API response into TranscriptionResult
     */
    private TranscriptionPort.TranscriptionResult parseTranscriptionResponse(
        String response,
        String responseFormat
    ) throws IOException {
        if ("text".equals(responseFormat)) {
            // Plain text response (no segments)
            return new TranscriptionPort.TranscriptionResult(
                response,
                List.of(),
                null,
                0.0
            );
        }

        // Parse JSON response
        JsonNode root = objectMapper.readTree(response);

        String fullText = root.path("text").asText();
        String detectedLanguage = root.path("language").asText(null);
        double duration = root.path("duration").asDouble(0.0);

        // Parse segments if available (verbose_json format)
        List<TranscriptionPort.TranscriptionSegment> segments = new ArrayList<>();
        if (root.has("segments") && root.get("segments").isArray()) {
            for (JsonNode segmentNode : root.get("segments")) {
                double start = segmentNode.path("start").asDouble();
                double end = segmentNode.path("end").asDouble();
                String text = segmentNode.path("text").asText();

                segments.add(new TranscriptionPort.TranscriptionSegment(start, end, text));
            }
        }

        log.info("Transcription completed: {} segments, {:.1f} seconds, language: {}",
            segments.size(), duration, detectedLanguage);

        return new TranscriptionPort.TranscriptionResult(
            fullText,
            segments,
            detectedLanguage,
            duration
        );
    }

    /**
     * Check if Whisper API is available
     */
    public boolean isAvailable() {
        return openAiApiKey != null && !openAiApiKey.isBlank();
    }
}
