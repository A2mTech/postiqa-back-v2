package fr.postiqa.core.domain.model;

/**
 * Metrics associated with a social media profile
 */
public record ProfileMetrics(
    Integer followers,
    Integer following,
    Integer totalPosts,
    Double averageEngagement,
    Integer totalLikes,
    Integer totalComments
) {
    public static ProfileMetrics empty() {
        return new ProfileMetrics(0, 0, 0, 0.0, 0, 0);
    }

    public static ProfileMetrics of(
        Integer followers,
        Integer following,
        Integer totalPosts,
        Integer totalLikes,
        Integer totalComments
    ) {
        double avgEngagement = calculateAverageEngagement(totalPosts, totalLikes, totalComments);
        return new ProfileMetrics(followers, following, totalPosts, avgEngagement, totalLikes, totalComments);
    }

    private static double calculateAverageEngagement(Integer totalPosts, Integer totalLikes, Integer totalComments) {
        if (totalPosts == null || totalPosts == 0) {
            return 0.0;
        }
        int totalEngagement = (totalLikes != null ? totalLikes : 0) +
                             (totalComments != null ? totalComments : 0);
        return (double) totalEngagement / totalPosts;
    }
}
