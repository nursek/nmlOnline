package com.mg.nmlonline;

import com.mg.nmlonline.domain.model.user.User;
import com.mg.nmlonline.domain.service.JwtService;
import com.mg.nmlonline.infrastructure.repository.UserRepository;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boote le profil {@code prod} sur un PostgreSQL 14 embarqué : couvre la configuration prod que le
 * profil {@code test} ne voit pas (beans {@code @Profile("prod")}, variables d'environnement).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@AutoConfigureEmbeddedDatabase(type = POSTGRES, provider = ZONKY)
class ProdBootParityTest {

    private static final long TOKEN_TTL_MS = 3_600_000;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;

    @DynamicPropertySource
    static void prodConfiguration(DynamicPropertyRegistry registry) {
        registry.add("jwt.secret", () -> "test-secret-key-for-ci-at-least-32-chars-long");
        registry.add("jwt.pepper", () -> "test-pepper-value-for-ci-tests-only");
        registry.add("app.admin.password", () -> "test-admin-password");
        // Peuple unités/bâtiments/personnages : sans ça le test d'endpoint ne prouve rien.
        registry.add("app.import-demo-data", () -> "true");
    }

    @Test
    void everyMigrationIsApplied(@Autowired JdbcTemplate jdbc) throws Exception {
        int onClasspath = new PathMatchingResourcePatternResolver()
                .getResources("classpath:db/migration/V*.sql").length;
        Integer applied = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where success = true", Integer.class);

        assertEquals(onClasspath, applied, "toutes les migrations du classpath doivent être appliquées");
    }

    /** Le DTO joueur parcourt des relations LAZY : 500 si le mapping sort de la transaction (OSIV). */
    @Test
    void adminApiListsPlayersOnMigratedSchema() throws Exception {
        User admin = userRepository.findByUsername("admin");
        String token = jwtService.generateToken(admin, TOKEN_TTL_MS);

        mockMvc.perform(get("/api/admin/players").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(greaterThan(0)));
    }
}
