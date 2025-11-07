package fr.postiqa.core.domain.model;

/**
 * Metrics associated with a social media post
 */
public record PostMetrics(
    Integer likes,
    Integer comments,
    Integer shares,
    Integer views,
    Double engagementRate
) {
    public static PostMetrics empty() {
        return new PostMetrics(0, 0, 0, 0, 0.0);
    }

    public static PostMetrics of(Integer likes, Integer comments, Integer shares, Integer views) {
        double engagement = calculateEngagementRate(likes, comments, shares, views);
        return new PostMetrics(likes, comments, shares, views, engagement);
    }

    private static double calculateEngagementRate(Integer likes, Integer comments, Integer shares, Integer views) {
        if (views == null || views == 0) {
            return 0.0;
        }
        int totalEngagement = (likes != null ? likes : 0) +
                             (comments != null ? comments : 0) +
                             (shares != null ? shares : 0);
        return (double) totalEngagement / views * 100;
    }
}
