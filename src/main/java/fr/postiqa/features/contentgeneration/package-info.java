/**
 * Feature content-generation - Génération de posts avec learning automatique.
 * <p>
 * Génère automatiquement des posts dans le style unique de l'utilisateur.
 * Apprend des modifications de l'utilisateur pour affiner le profil.
 * <p>
 * Architecture Clean Architecture :
 * - domain : Modèle métier de génération
 * - usecase : Logique de génération et learning
 * - adapter : Exposition de la feature
 * - infrastructure : Clients OpenAI/Anthropic
 */
@org.springframework.modulith.ApplicationModule
@org.springframework.lang.NonNullApi
@org.springframework.lang.NonNullFields
package fr.postiqa.features.contentgeneration;
