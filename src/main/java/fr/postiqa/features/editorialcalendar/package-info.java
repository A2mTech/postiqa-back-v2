/**
 * Feature editorial-calendar - Stratégie éditoriale dynamique.
 * <p>
 * Génère et adapte une stratégie éditoriale hebdomadaire basée sur :
 * - Le profil d'écriture de l'utilisateur
 * - Le business détecté (SaaS, e-commerce, etc.)
 * - Les objectifs par plateforme
 * <p>
 * La stratégie évolue chaque semaine en fonction des performances.
 * <p>
 * Architecture Clean Architecture :
 * - domain : Modèle métier de stratégie éditoriale
 * - usecase : Logique de génération et adaptation de stratégie
 * - adapter : Exposition de la feature
 * - infrastructure : Clients IA et analyse de données
 */
@org.springframework.modulith.ApplicationModule
@org.springframework.lang.NonNullApi
@org.springframework.lang.NonNullFields
package fr.postiqa.features.editorialcalendar;
