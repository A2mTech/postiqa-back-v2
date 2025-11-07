/**
 * Module agency - API REST pour les agences.
 * <p>
 * Expose les endpoints REST sous /api/agency pour les agences
 * qui gèrent plusieurs clients.
 * <p>
 * Ce module réutilise les features en mode multi-clients et
 * ne contient AUCUNE logique métier. Il orchestre simplement
 * les appels aux features avec un contexte multi-tenant.
 * <p>
 * Architecture Spring classique avec controllers REST.
 */
@org.springframework.modulith.ApplicationModule
@org.springframework.lang.NonNullApi
@org.springframework.lang.NonNullFields
package fr.postiqa.agency;
