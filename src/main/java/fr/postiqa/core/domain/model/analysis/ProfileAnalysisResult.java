package fr.postiqa.core.domain.model.analysis;

import fr.postiqa.core.domain.enums.SocialPlatform;

import java.util.List;
import java.util.Map;

/**
 * Complete profile analysis for a social media platform.
 * Combines profile picture, banner, and bio analysis.
 * Maps to Phase 2B in the ultra-deep analysis workflow.
 */
public record ProfileAnalysisResult(
    SocialPlatform platform,
    String profileUrl,
    String username,
    String displayName,
    String bio,
    int followerCount,
    int followingCount,
    ProfilePictureAnalysis profilePictureAnalysis,
    BannerAnalysis bannerAnalysis,
    BioAnalysis bioAnalysis,
    Map<String, Object> rawProfileData
) {
    public boolean hasProfilePicture() {
        return profilePictureAnalysis != null;
    }

    public boolean hasBanner() {
        return bannerAnalysis != null && bannerAnalysis.hasBanner();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private SocialPlatform platform;
        private String profileUrl;
        private String username;
        private String displayName;
        private String bio;
        private int followerCount;
        private int followingCount;
        private ProfilePictureAnalysis profilePictureAnalysis;
        private BannerAnalysis bannerAnalysis;
        private BioAnalysis bioAnalysis;
        private Map<String, Object> rawProfileData;

        public Builder platform(SocialPlatform platform) {
            this.platform = platform;
            return this;
        }

        public Builder profileUrl(String profileUrl) {
            this.profileUrl = profileUrl;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder bio(String bio) {
            this.bio = bio;
            return this;
        }

        public Builder followerCount(int followerCount) {
            this.followerCount = followerCount;
            return this;
        }

        public Builder followingCount(int followingCount) {
            this.followingCount = followingCount;
            return this;
        }

        public Builder profilePictureAnalysis(ProfilePictureAnalysis profilePictureAnalysis) {
            this.profilePictureAnalysis = profilePictureAnalysis;
            return this;
        }

        public Builder bannerAnalysis(BannerAnalysis bannerAnalysis) {
            this.bannerAnalysis = bannerAnalysis;
            return this;
        }

        public Builder bioAnalysis(BioAnalysis bioAnalysis) {
            this.bioAnalysis = bioAnalysis;
            return this;
        }

        public Builder rawProfileData(Map<String, Object> rawProfileData) {
            this.rawProfileData = rawProfileData;
            return this;
        }

        public ProfileAnalysisResult build() {
            return new ProfileAnalysisResult(
                platform,
                profileUrl,
                username,
                displayName,
                bio,
                followerCount,
                followingCount,
                profilePictureAnalysis,
                bannerAnalysis,
                bioAnalysis,
                rawProfileData
            );
        }
    }
}

/**
 * Profile picture vision analysis result
 */
record ProfilePictureAnalysis(
    String type,  // person, logo, illustration, other
    String description,
    List<String> colors,
    String style,  // professional, casual, artistic
    String brandAlignment
) {}

/**
 * Banner vision analysis result
 */
record BannerAnalysis(
    boolean hasBanner,
    String textOverlay,
    String textPosition,
    String imagery,
    boolean ctaVisible,
    String ctaText,
    List<String> colors,
    String style,
    int brandConsistencyScore,  // 1-10
    int messageClarity,  // 1-10
    List<String> recommendations
) {}

/**
 * Bio text analysis result
 */
record BioAnalysis(
    int clarityScore,  // 1-10
    boolean valuePropositionPresent,
    boolean ctaPresent,
    List<String> keywords,
    List<String> tone,  // professional, casual, humorous
    int brandConsistencyScore,  // 1-10
    int messageCoherence,  // 1-10
    List<String> optimizationSuggestions
) {}
