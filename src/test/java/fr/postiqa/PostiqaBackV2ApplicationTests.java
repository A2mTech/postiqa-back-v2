package fr.postiqa;

import org.junit.jupiter.api.Test;

/**
 * Test de base de l'application.
 * <p>
 * Le test de chargement complet du contexte sera activé une fois
 * que la base de données, Redis et les APIs externes seront configurés.
 */
class PostiqaBackV2ApplicationTests {

    @Test
    void applicationClassExists() {
        // Test basique vérifiant que la classe principale existe
        assert PostiqaBackV2Application.class != null;
    }

}
