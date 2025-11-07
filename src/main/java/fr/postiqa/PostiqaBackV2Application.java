package fr.postiqa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulith;

/**
 * Application principale Postiqa - Monolithe modulaire.
 * <p>
 * Architecture Spring Modulith avec modules :
 * - shared, database, gateway (modules socle)
 * - core (orchestration scraping + analyse - Clean Archi)
 * - features (content-generation, editorial-calendar, publishing, weekly-brief, analytics)
 * - business, agency (modules API REST)
 */
@Modulith(
        systemName = "Postiqa",
        sharedModules = {"shared", "database"}
)
@SpringBootApplication
public class PostiqaBackV2Application {

    public static void main(String[] args) {
        SpringApplication.run(PostiqaBackV2Application.class, args);
    }

}
