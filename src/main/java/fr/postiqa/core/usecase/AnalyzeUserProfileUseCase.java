package fr.postiqa.core.usecase;

import fr.postiqa.core.domain.enums.SocialPlatform;
import fr.postiqa.core.domain.model.SocialPost;
import fr.postiqa.core.domain.port.AnalysisPort;
import fr.postiqa.core.domain.port.ScrapingPort;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Use case pour analyser le profil complet d'un utilisateur.
 * <p>
 * Orchestre le scraping des posts et l'analyse pour créer le profil d'écriture.
 */
@Component
public class AnalyzeUserProfileUseCase {

    private final ScrapingPort scrapingPort;
    private final AnalysisPort analysisPort;

    public AnalyzeUserProfileUseCase(ScrapingPort scrapingPort, AnalysisPort analysisPort) {
        this.scrapingPort = scrapingPort;
        this.analysisPort = analysisPort;
    }

    /**
     * Analyse le profil d'un utilisateur sur toutes les plateformes.
     *
     * @param userId identifiant utilisateur
     * @return profil d'écriture généré
     */
    public AnalysisPort.WritingProfile execute(String userId) {
        // 1. Scrape posts from all platforms (using new API)
        var linkedinPosts = scrapingPort.scrapePosts(
            SocialPlatform.LINKEDIN,
            userId,
            50 // Max 50 posts
        );
        var twitterPosts = scrapingPort.scrapePosts(
            SocialPlatform.TWITTER,
            userId,
            50
        );
        var instagramPosts = scrapingPort.scrapePosts(
            SocialPlatform.INSTAGRAM,
            userId,
            50
        );

        // 2. Extract post contents from SocialPost objects
        List<String> allPostContents = List.of(
            linkedinPosts, twitterPosts, instagramPosts
        ).stream()
            .flatMap(List::stream)
            .map(SocialPost::content)
            .filter(content -> content != null && !content.isBlank())
            .toList();

        // 3. Analyze writing style
        return analysisPort.analyzeWritingStyle(allPostContents);
    }
}
