package fr.postiqa.core.domain.port;

import java.util.List;

/**
 * Port pour l'analyse de posts et création du profil d'écriture.
 * <p>
 * Définit le contrat pour analyser les posts et extraire le style unique
 * de l'utilisateur via des APIs IA externes (OpenAI, Anthropic).
 */
public interface AnalysisPort {

    /**
     * Analyse un ensemble de posts pour créer un profil d'écriture.
     *
     * @param posts posts à analyser
     * @return profil d'écriture généré
     */
    WritingProfile analyzeWritingStyle(List<String> posts);

    /**
     * Représente le profil d'écriture d'un utilisateur.
     */
    record WritingProfile(
            String tone,
            String narrativeStructure,
            List<String> commonHooks,
            String emojiUsage,
            String vocabulary
    ) {}
}
