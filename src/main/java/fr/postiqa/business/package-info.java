/**
 * Module business - API REST pour les entreprises.
 * <p>
 * Expose les endpoints REST sous /api/business pour les entreprises
 * qui gèrent leur propre compte social media.
 * <p>
 * Ce module orchestre les features dont il a besoin et ne contient
 * AUCUNE logique métier. Il se contente de coordonner les appels
 * aux différentes features.
 * <p>
 * Architecture Spring classique avec controllers REST.
 */
@org.springframework.modulith.ApplicationModule
@org.springframework.lang.NonNullApi
@org.springframework.lang.NonNullFields
package fr.postiqa.business;
