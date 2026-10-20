package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.EmbeddedPostgresTest;
import com.mg.nmlonline.api.dto.ScenarioSummaryDto;
import com.mg.nmlonline.domain.model.action.PlayerActionStatus;
import com.mg.nmlonline.infrastructure.repository.AllianceRepository;
import com.mg.nmlonline.infrastructure.repository.AnnouncementRepository;
import com.mg.nmlonline.infrastructure.repository.BattleReportRepository;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.MovementOrderRepository;
import com.mg.nmlonline.infrastructure.repository.PendingCaptureRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerActionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Démo jouée de bout en bout sur les fixtures : le script doit aller jusqu'au tour 3 et refuser
 * une partie déjà entamée (sinon les ventes/ordres seraient rejoués sur un monde déjà joué).
 */
@EmbeddedPostgresTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DisplayName("Démo — deux tours joués depuis les fixtures")
class DemoTurnsSeederTest {

    @Autowired
    private DemoTurnsSeeder seeder;

    @Autowired
    private TurnResolutionOrchestrator orchestrator;

    @Autowired
    private TurnService turnService;

    @Autowired
    private PlayerActionRepository playerActionRepository;

    @Autowired
    private BattleReportRepository battleReportRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private PlatformTransactionManager txManager;

    @Autowired
    private EntityManager em;

    @Autowired
    private MovementOrderRepository movementOrderRepository;

    @Autowired
    private AllianceRepository allianceRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private PendingCaptureRepository pendingCaptureRepository;

    // Base partagée par toute la suite : la démo committe ventes/ordres/rapports, il faut les purger.
    @AfterEach
    void cleanUpSharedDatabase() {
        orchestrator.abort();
        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            playerActionRepository.deleteAll();
            movementOrderRepository.deleteAll();
            battleReportRepository.deleteAll();
            allianceRepository.deleteAll();
            announcementRepository.deleteAll();
            pendingCaptureRepository.deleteAll();
            boardRepository.findAll().stream().findFirst().ifPresent(board -> {
                board.setCurrentTurn(1);
                board.setRevenueClaimedTurn(null);
            });
            em.flush();
        });
        turnService.invalidateTurnCache();
    }

    @Test
    @DisplayName("les tours 1 et 2 joués mènent au tour 3, puis la démo refuse de rejouer")
    void playDemoTurns_vaJusquAuTour3_etRefuseDeRejouer() {
        ScenarioSummaryDto report = seeder.playDemoTurns();

        assertEquals(3, report.getTurn(), "La démo joue les tours 1 et 2");
        assertEquals(turnService.getCurrentTurn(), report.getTurn());
        assertTrue(report.getMessage().contains("capture(s) au tour 1"),
                "Le rapport compte les captures : " + report.getMessage());
        assertTrue(battleReportRepository.count() >= 1,
                "La bataille du secteur 43 doit produire un rapport de combat");
        assertTrue(playerActionRepository.count() > 0,
                "Ventes, équipements et récoltes passent par le journal d'actions");

        long captured = new TransactionTemplate(txManager).execute(status -> boardRepository.findAll().stream()
                .findFirst().orElseThrow()
                .getAllSectors().stream().filter(sector -> !sector.isNeutral()).count());
        assertTrue(captured >= 35,
                "La vague de conquêtes des deux tours couvre presque la carte, got " + captured);
        assertTrue(playerActionRepository.findByTurnAndStatus(3, PlayerActionStatus.ACTIVE).isEmpty(),
                "Le tour 3 reste vierge : la démo ne joue que deux tours");

        IllegalStateException replay = assertThrows(IllegalStateException.class, seeder::playDemoTurns);
        assertTrue(replay.getMessage().contains("partie vierge"),
                "Le refus explique qu'il faut une partie vierge : " + replay.getMessage());
    }
}
