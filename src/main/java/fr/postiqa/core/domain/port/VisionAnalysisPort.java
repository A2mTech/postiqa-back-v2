package fr.postiqa.core.domain.port;

import java.util.List;
import java.util.Map;

/**
 * Port for AI-powered vision analysis using GPT-4 Vision or similar models.
 * <p>
 * Supports analyzing images, video frames, and visual content from social media posts.
 * Used for profile pictures, banners, image posts, carousels, and video analysis.
 */
public interface VisionAnalysisPort {

    /**
     * Analyze a single image with a custom prompt.
     *
     * @param imageUrl URL of the image to analyze
     * @param prompt Custom analysis prompt
     * @return Analysis result as structured data
     */
    Map<String, Object> analyzeImage(String imageUrl, String prompt);

    /**
     * Analyze multiple images in batch with a custom prompt.
     * Useful for carousels or comparing multiple images.
     *
     * @param imageUrls List of image URLs to analyze
     * @param prompt Custom analysis prompt (can reference multiple images)
     * @return Analysis result as structured data
     */
    Map<String, Object> analyzeImages(List<String> imageUrls, String prompt);

    /**
     * Analyze an image with text context (multimodal).
     * Combines text and visual analysis.
     *
     * @param imageUrl URL of the image
     * @param textContent Associated text content
     * @param prompt Custom analysis prompt
     * @return Analysis result as structured data
     */
    Map<String, Object> analyzeImageWithText(String imageUrl, String textContent, String prompt);

    /**
     * Analyze a video frame with context.
     * Used for analyzing key frames extracted from videos.
     *
     * @param frameUrl URL of the video frame
     * @param timestamp Timestamp in the video (seconds)
     * @param transcriptSegment Transcript text for this time segment
     * @param prompt Custom analysis prompt
     * @return Analysis result as structured data
     */
    Map<String, Object> analyzeVideoFrame(
        String frameUrl,
        double timestamp,
        String transcriptSegment,
        String prompt
    );

    /**
     * Analyze multiple video frames to understand video narrative.
     * Useful for extracting visual storytelling patterns.
     *
     * @param frames List of frame data (URL, timestamp, transcript segment)
     * @param prompt Custom analysis prompt
     * @return Analysis result as structured data
     */
    Map<String, Object> analyzeVideoFrames(List<VideoFrame> frames, String prompt);

    /**
     * Extract text from image using OCR (Optical Character Recognition).
     * Useful for carousel slides with text overlays.
     *
     * @param imageUrl URL of the image
     * @return Extracted text content
     */
    String extractTextFromImage(String imageUrl);

    /**
     * Represents a video frame with metadata
     */
    record VideoFrame(
        String frameUrl,
        double timestamp,
        String transcriptSegment,
        String position  // opening, middle, ending
    ) {}
}
