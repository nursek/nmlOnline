package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.EmbeddedPostgresTest;
import com.mg.nmlonline.api.dto.PendingConflictDto;
import com.mg.nmlonline.api.dto.ResolvedBattleDto;
import com.mg.nmlonline.api.dto.ScenarioSummaryDto;
import com.mg.nmlonline.api.dto.TurnResolutionStateDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.EntityCategory;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@EmbeddedPostgresTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DisplayName("TurnResolutionOrchestrator — impasse mexicaine 3 camps")
class TurnResolutionOrchestratorStandoffTest {

    @Autowired
    private TurnResolutionScenarioSeeder seeder;

    @Autowired
    private TurnResolutionOrchestrator orchestrator;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private EntityManager em;

    @AfterEach
    void releaseLock() {
        orchestrator.abort();
    }

    @Test
    @Transactional
    @DisplayName("seed + 1 hop : un seul conflit impasse ordonné [défenseur, imotekh, lurio] puis résolution")
    void seedStandoff_puisHop_puisResolution() {
        ScenarioSummaryDto seed = seeder.seedStandoffScenario();
        em.flush();

        assertTrue(seed.isStandoff(), "Le seeder doit marquer le scénario comme impasse");
        assertEquals(2, seed.getOrders().size(), "Deux attaquants : imotekh puis lurio");
        Long cegorachId = seed.getDefender().getId();
        Long imotekhId = seed.getOrders().get(0).getPlayerId();
        Long lurioId = seed.getOrders().get(1).getPlayerId();

        TurnResolutionStateDto state = orchestrator.startSession();
        assertEquals(1, state.getMaxSteps(), "Les deux routes [43,32] et [41,32] font 1 hop");

        state = orchestrator.advanceHop();

        assertEquals(1, state.getPendingConflicts().size(), "Un unique conflit groupé pour le secteur");
        PendingConflictDto pc = state.getPendingConflicts().getFirst();
        assertEquals(32, pc.getSectorNumber());
        assertTrue(pc.isStandoff(), "3 camps au secteur : impasse");
        assertEquals(List.of(cegorachId, imotekhId, lurioId),
                pc.getParticipants().stream().map(PendingConflictDto.ParticipantDto::getPlayerId).toList(),
                "Défenseur en tête, puis arrivants par ordre d'envoi");
        assertNull(pc.getParticipants().get(0).getSubmittedAt(), "Un défenseur stationnaire n'a pas d'ordre");
        assertNotNull(pc.getParticipants().get(1).getSubmittedAt());
        assertNotNull(pc.getParticipants().get(2).getSubmittedAt());

        ResolvedBattleDto report = orchestrator.resolveBattle(pc.getConflictId());

        assertTrue(report.isSuccess());
        assertTrue(report.isStandoff());
        assertEquals(3, report.getParticipants().size());
        assertEquals(List.of(cegorachId, imotekhId, lurioId),
                report.getParticipants().stream()
                        .map(ResolvedBattleDto.StandoffParticipantDto::getPlayerId).toList());
        if (report.getWinnerId() != null) {
            long vainqueurs = report.getParticipants().stream()
                    .filter(p -> p.getPlayerId().equals(report.getWinnerId()))
                    .count();
            assertEquals(1, vainqueurs, "Au plus un camp peut être vainqueur");
        }

        assertTrue(report.getBattleLog().stream()
                        .anyMatch(e -> "État initial".equals(e.getPhase()) && e.getMessage().contains("Atk")),
                "Le journal expose l'état initial des camps avec leurs statistiques");
        assertTrue(report.getBattleLog().stream()
                        .anyMatch(e -> ("DAMAGE".equals(e.getOutcome()) || "DESTROYED".equals(e.getOutcome()))
                                && e.getMessage().contains(" · ")),
                "Chaque frappe nomme l'unité ciblée et son propriétaire");
        assertTrue(report.getBattleLog().stream()
                        .anyMatch(e -> "Résultat".equals(e.getPhase()) && e.getMessage().startsWith("Pertes")),
                "Le journal se termine par le bilan des pertes");

        assertEquals(0, orchestrator.getState().getPendingConflicts().size());
        assertTrue(orchestrator.getState().isCanFinalize());
        assertNotNull(orchestrator.finalizeTurn());

        em.flush();
        em.clear();
        Board board = boardRepository.findAll().stream().findFirst().orElseThrow();
        Sector secteur32 = board.getSector(32);
        long campsAvecCombattants = List.of(cegorachId, imotekhId, lurioId).stream()
                .filter(playerId -> secteur32.getCombatEntities().stream()
                        .anyMatch(e -> playerId.equals(e.getPlayerId())
                                && (e.getEntityCategory() == EntityCategory.INFANTRY
                                    || e.getEntityCategory() == EntityCategory.CHARACTER)))
                .count();
        if (report.getWinnerId() != null) {
            assertEquals(1, campsAvecCombattants,
                    "Un vainqueur unique : seul son camp conserve des combattants");
        } else {
            assertNotEquals(1, campsAvecCombattants,
                    "Sans vainqueur, aucun camp unique ne survit (0 ou 2+)");
        }
    }
}
