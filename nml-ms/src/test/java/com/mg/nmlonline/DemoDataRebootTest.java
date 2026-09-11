package com.mg.nmlonline;

import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.service.PlayerService;
import com.mg.nmlonline.infrastructure.repository.EquipmentRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerResourceRepository;
import com.mg.nmlonline.infrastructure.repository.SectorRepository;
import com.mg.nmlonline.infrastructure.repository.VehicleRepository;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Deux démarrages prod successifs sur la même base : le second doit reset, recharger le
 * catalogue et réimporter les joueurs. Couvre le bug détecté uniquement en prod où
 * {@code saveBoard} rend des secteurs détachés au second boot.
 */
class DemoDataRebootTest {

    private static EmbeddedPostgres postgres;

    @BeforeAll
    static void startPostgres() throws Exception {
        postgres = EmbeddedPostgres.builder().start();
    }

    @AfterAll
    static void stopPostgres() throws Exception {
        postgres.close();
    }

    @Test
    void secondBootOnExistingDataImportsCleanly() {
        String jdbcUrl = postgres.getJdbcUrl("postgres", "postgres");

        try (ConfigurableApplicationContext first = boot(jdbcUrl)) {
            assertTrue(first.getBean(EquipmentRepository.class).findByName("Gauss Blaster").isPresent());
        }

        try (ConfigurableApplicationContext second = boot(jdbcUrl)) {
            assertTrue(second.getBean(EquipmentRepository.class).findByName("Gauss Blaster").isPresent());
            assertTrue(second.getBean(PlayerResourceRepository.class).count() > 0);

            Player trazyn = second.getBean(PlayerService.class).findByName("trazyn");
            assertNotNull(trazyn);
            assertFalse(second.getBean(VehicleRepository.class).findByPlayerId(trazyn.getId()).isEmpty());
            assertFalse(second.getBean(SectorRepository.class).findByOwnerId(trazyn.getId()).isEmpty());
        }
    }

    private ConfigurableApplicationContext boot(String jdbcUrl) {
        return new SpringApplicationBuilder(NmlOnlineApplication.class)
                .profiles("prod")
                .web(WebApplicationType.SERVLET)
                .run(
                        "--server.port=0",
                        "--spring.datasource.url=" + jdbcUrl,
                        "--spring.datasource.username=postgres",
                        "--spring.datasource.password=",
                        "--jwt.secret=test-secret-key-for-ci-at-least-32-chars-long",
                        "--jwt.pepper=test-pepper-value-for-ci-tests-only",
                        "--app.admin.password=test-admin-password",
                        "--app.import-demo-data=true");
    }
}
