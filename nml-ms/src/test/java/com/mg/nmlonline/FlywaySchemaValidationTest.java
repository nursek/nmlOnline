package com.mg.nmlonline;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Garde-fou schéma : applique V1..V7 sur un vrai PostgreSQL puis démarre le contexte
 * avec le profil prod, donc {@code ddl-auto=validate}. Si une entité diverge du schéma
 * Flyway, Hibernate refuse de démarrer et le test échoue — avant la prod, pas pendant.
 *
 * <p>{@code disabledWithoutDocker} : sauté en local sans Docker, exécuté sur les runners CI.</p>
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("prod")
class FlywaySchemaValidationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("jwt.secret", () -> "test-secret-key-for-ci-at-least-32-chars-long");
        registry.add("jwt.pepper", () -> "test-pepper-value-for-ci-tests-only");
        registry.add("app.admin.password", () -> "test-admin-password");
        registry.add("app.import-demo-data", () -> "false");
    }

    @Test
    void everyMigrationIsApplied(@Autowired JdbcTemplate jdbc) throws Exception {
        int onClasspath = new PathMatchingResourcePatternResolver()
                .getResources("classpath:db/migration/V*.sql").length;
        Integer applied = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where success = true", Integer.class);

        assertEquals(onClasspath, applied, "toutes les migrations du classpath doivent être appliquées");
    }
}
