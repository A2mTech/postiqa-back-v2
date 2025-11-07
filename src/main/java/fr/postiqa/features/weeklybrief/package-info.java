/**
 * Feature weekly-brief - Brief hebdomadaire vocal.
 * <p>
 * Feature clé permettant à l'utilisateur d'enregistrer vocalement
 * les événements de sa semaine. Le système transcrit, extrait les
 * événements et génère automatiquement des posts dans son style.
 * <p>
 * Workflow :
 * 1. Enregistrement vocal de l'utilisateur
 * 2. Transcription via Whisper API
 * 3. Extraction des événements via IA
 * 4. Génération automatique de posts
 * 5. Programmation avec validation utilisateur
 * <p>
 * Architecture Spring classique avec services et controllers internes.
 */
@org.springframework.modulith.ApplicationModule
@org.springframework.lang.NonNullApi
@org.springframework.lang.NonNullFields
package fr.postiqa.features.weeklybrief;
