package fr.postiqa.core.adapter.out;

import fr.postiqa.core.domain.port.VisionAnalysisPort;
import fr.postiqa.core.infrastructure.client.GPT4VisionClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * OpenAI GPT-4 Vision implementation of VisionAnalysisPort.
 * <p>
 * Provides AI-powered vision analysis using GPT-4 Vision API.
 * Supports image analysis, OCR, video frame analysis, and multimodal understanding.
 */
@Component
public class OpenAIVisionAdapter implements VisionAnalysisPort {

    private static final Logger log = LoggerFactory.getLogger(OpenAIVisionAdapter.class);

    private final GPT4VisionClient visionClient;

    public OpenAIVisionAdapter(GPT4VisionClient visionClient) {
        this.visionClient = visionClient;
    }

    @Override
    public Map<String, Object> analyzeImage(String imageUrl, String prompt) {
        log.info("Analyzing image with GPT-4 Vision: {}", imageUrl);
        return visionClient.analyzeImage(imageUrl, prompt);
    }

    @Override
    public Map<String, Object> analyzeImages(List<String> imageUrls, String prompt) {
        log.info("Analyzing {} images with GPT-4 Vision", imageUrls.size());
        return visionClient.analyzeImages(imageUrls, prompt);
    }

    @Override
    public Map<String, Object> analyzeImageWithText(String imageUrl, String textContent, String prompt) {
        log.info("Analyzing image with text context: {}", imageUrl);
        return visionClient.analyzeImageWithText(imageUrl, textContent, prompt);
    }

    @Override
    public Map<String, Object> analyzeVideoFrame(
        String frameUrl,
        double timestamp,
        String transcriptSegment,
        String prompt
    ) {
        log.info("Analyzing video frame at timestamp {} seconds", timestamp);
        return visionClient.analyzeVideoFrame(frameUrl, timestamp, transcriptSegment, prompt);
    }

    @Override
    public Map<String, Object> analyzeVideoFrames(List<VideoFrame> frames, String prompt) {
        log.info("Analyzing {} video frames", frames.size());
        return visionClient.analyzeVideoFrames(frames, prompt);
    }

    @Override
    public String extractTextFromImage(String imageUrl) {
        log.info("Extracting text from image (OCR): {}", imageUrl);
        return visionClient.extractText(imageUrl);
    }
}
