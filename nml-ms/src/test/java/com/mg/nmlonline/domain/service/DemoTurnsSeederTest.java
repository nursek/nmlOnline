package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.DemoDatabaseReset;
import com.mg.nmlonline.DemoFixtures;
import com.mg.nmlonline.EmbeddedPostgresTest;
import com.mg.nmlonline.api.dto.ScenarioSummaryDto;
import com.mg.nmlonline.domain.model.action.PlayerAction;
import com.mg.nmlonline.domain.model.action.PlayerActionStatus;
import com.mg.nmlonline.domain.model.action.PlayerActionType;
import com.mg.nmlonline.infrastructure.repository.BattleReportRepository;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerActionRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
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
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Démo — deux tours joués depuis les fixtures")
class DemoTurnsSeederTest extends DemoDatabaseReset {

    @Autowired
    private DemoTurnsSeeder seeder;

    @Autowired
    private TurnService turnService;

    @Autowired
    private PlayerActionRepository playerActionRepository;

    @Autowired
    private BattleReportRepository battleReportRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private PlatformTransactionManager txManager;

    private Long boardId() {
        return DemoFixtures.mainBoardId(boardRepository);
    }

    @Test
    @DisplayName("les tours 1 et 2 joués mènent au tour 3, puis la démo refuse de rejouer")
    void playDemoTurns_vaJusquAuTour3_etRefuseDeRejouer() {
        ScenarioSummaryDto report = seeder.playDemoTurns(boardId());

        assertEquals(3, report.getTurn(), "La démo joue les tours 1 et 2");
        assertEquals(turnService.getCurrentTurn(boardId()), report.getTurn());
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
        assertTrue(playerActionRepository.findByBoardIdAndTurnAndStatus(
                        boardId(), 3, PlayerActionStatus.ACTIVE).isEmpty(),
                "Le tour 3 reste vierge : la démo ne joue que deux tours");

        IllegalStateException replay = assertThrows(IllegalStateException.class, () -> seeder.playDemoTurns(boardId()));
        assertTrue(replay.getMessage().contains("partie vierge"),
                "Le refus explique qu'il faut une partie vierge : " + replay.getMessage());
    }

    @Test
    @DisplayName("refuse une partie autre que la carte de démo scriptée")
    void playDemoTurns_refuseUneAutrePartie() {
        Long arcsBoardId = boardRepository.findByName("Carte 6 Joueurs (Arcs)").orElseThrow().getId();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> seeder.playDemoTurns(arcsBoardId));
        assertTrue(error.getMessage().contains("Carte Principale"), error.getMessage());
    }

    @Test
    @DisplayName("une action sur une autre partie ne bloque pas la démo scriptée")
    void playDemoTurns_ignoreLesActionsDesAutresParties() {
        Long arcsBoardId = boardRepository.findByName("Carte 6 Joueurs (Arcs)").orElseThrow().getId();
        PlayerAction foreignAction = new PlayerAction();
        foreignAction.setPlayerId(playerRepository.findByBoardId(arcsBoardId).getFirst().getId());
        foreignAction.setBoardId(arcsBoardId);
        foreignAction.setTurn(1);
        foreignAction.setType(PlayerActionType.HARVEST_RESOURCE);
        new TransactionTemplate(txManager).executeWithoutResult(status -> playerActionRepository.save(foreignAction));

        ScenarioSummaryDto report = seeder.playDemoTurns(boardId());

        assertEquals(3, report.getTurn());
        assertTrue(playerActionRepository.countByBoardId(boardId()) > 0);
    }
}
