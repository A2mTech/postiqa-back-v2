package fr.postiqa;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Tests de vérification de l'architecture modulaire.
 * <p>
 * Ces tests vérifient que :
 * - Les modules respectent leurs frontières
 * - Les dépendances entre modules sont conformes à l'architecture
 * - Aucun cycle de dépendances n'existe
 */
class ModularityTests {

    private static final ApplicationModules modules = ApplicationModules.of(PostiqaBackV2Application.class);

    /**
     * Vérifie que tous les modules respectent leurs frontières.
     * <p>
     * Échoue si un module accède à un package interne d'un autre module
     * sans passer par son API publique.
     */
    @Test
    void verifyModularity() {
        modules.verify();
    }

    /**
     * Vérifie qu'il n'y a pas de cycles de dépendances entre modules.
     * <p>
     * Un cycle rendrait impossible l'extraction future d'un module
     * en micro-service indépendant.
     */
    @Test
    void verifyNoCycles() {
        // La vérification des cycles est incluse dans verify()
        modules.verify();
    }

    /**
     * Génère la documentation de l'architecture modulaire.
     * <p>
     * Crée des diagrammes PlantUML et AsciiDoc dans target/modulith-docs/
     */
    @Test
    void generateModularityDocumentation() {
        new Documenter(modules)
                .writeDocumentation()
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml();
    }

    /**
     * Affiche la structure des modules détectés.
     * <p>
     * Utile pour debug et compréhension de l'architecture.
     */
    @Test
    void printModuleStructure() {
        System.out.println("\n=== Structure des modules Postiqa ===\n");
        modules.forEach(module -> {
            System.out.println("Module: " + module.getName());
            System.out.println("  Base package: " + module.getBasePackage());
            System.out.println("  Display name: " + module.getDisplayName());
            System.out.println();
        });
    }
}
