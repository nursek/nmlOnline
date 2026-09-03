package com.mg.nmlonline;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES;

/**
 * Test d'intégration sur un vrai PostgreSQL 14 : mêmes migrations Flyway et même moteur qu'en prod.
 *
 * <p>Une seule base pour toute la suite, réensemencée à chaque création de contexte. Ne pas ajouter
 * de {@code refresh} : la base serait vidée sans que {@code PlayerStartupImporter} ne rejoue.</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureEmbeddedDatabase(type = POSTGRES, provider = ZONKY)
public @interface EmbeddedPostgresTest {
}
