package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.EmbeddedPostgresTest;
import com.mg.nmlonline.api.dto.ScenarioSummaryDto;
import com.mg.nmlonline.domain.model.alliance.Alliance;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.repository.AllianceRepository;
import com.mg.nmlonline.DemoFixtures;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EmbeddedPostgresTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DisplayName("FeatureScenarioSeeder — alliance et vente rejouables")
class FeatureScenarioSeederTest {

    private Long boardId() {
        return DemoFixtures.mainBoardId(boardRepository);
    }

    @Autowired
    private FeatureScenarioSeeder seeder;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private AllianceRepository allianceRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private PlatformTransactionManager txManager;

    @Test
    @DisplayName("alliance : formée une fois puis signalée déjà active, sans doublon")
    void allianceSeedIsIdempotent() {
        ScenarioSummaryDto first = seeder.seedAllianceScenario(boardId());
        assertTrue(first.getMessage().contains("Alliance formée"));

        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            Player mortarion = playerRepository.findByBoardIdAndName(boardId(), "mortarion").orElseThrow();
            Player angron = playerRepository.findByBoardIdAndName(boardId(), "angron").orElseThrow();
            assertTrue(allianceRepository.findActiveBetween(mortarion.getId(), angron.getId()).isPresent());
            List<Alliance> active = allianceRepository.findAllActiveByBoardId(boardId()).stream()
                    .filter(alliance -> alliance.involves(mortarion.getId()))
                    .filter(alliance -> alliance.involves(angron.getId()))
                    .toList();
            assertEquals(1, active.size(), "Un seul enregistrement d'alliance pour la paire");
        });

        ScenarioSummaryDto second = seeder.seedAllianceScenario(boardId());
        assertTrue(second.getMessage().contains("déjà active"));
    }

    @Test
    @DisplayName("vente : réapprovisionne le stock épuisé et crédite le joueur à chaque seed")
    void saleSeedReplenishesStock() {
        Player before = playerRepository.findByBoardIdAndName(boardId(), "lurio").orElseThrow();
        double moneyBefore = before.getStats().getMoney();

        ScenarioSummaryDto first = seeder.seedResourceSaleScenario(boardId());
        assertTrue(first.getMessage().contains("Caisse d'armes"));
        seeder.seedResourceSaleScenario(boardId());
        ScenarioSummaryDto third = seeder.seedResourceSaleScenario(boardId());
        assertTrue(third.getMessage().contains("Caisse d'armes"),
                "Le troisième seed doit réapprovisionner un stock tombé à zéro");

        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            em.clear();
            Player lurio = playerRepository.findByBoardIdAndName(boardId(), "lurio").orElseThrow();
            assertNotNull(lurio.getUserId());
            assertTrue(lurio.getStats().getMoney() > moneyBefore,
                    "Les ventes ont crédité lurio");
        });
    }
}
