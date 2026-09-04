package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.EmbeddedPostgresTest;
import com.mg.nmlonline.api.dto.PendingConflictDto;
import com.mg.nmlonline.api.dto.ResolvedBattleDto;
import com.mg.nmlonline.api.dto.ScenarioSummaryDto;
import com.mg.nmlonline.api.dto.TurnFinalizeResultDto;
import com.mg.nmlonline.api.dto.TurnResolutionStateDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.movement.MovementOrder;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.MovementOrderRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

// Scénario déterministe : BRUTE 100/100 vs LARBIN 10/10, pas d'évasion (pas de RNG).
@EmbeddedPostgresTest
@DisplayName("TurnResolutionOrchestrator — étapes du cycle pas-à-pas")
class TurnResolutionOrchestratorStepsTest {

    @Autowired
    private TurnResolutionOrchestrator orchestrator;

    @Autowired
    private TurnService turnService;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private MovementOrderRepository movementOrderRepository;

    @Autowired
    private EntityManager em;

    @SpyBean
    private MovementService movementService;

    @Autowired
    private TurnLock turnLock;

    @Autowired
    private TurnResolutionScenarioSeeder seeder;

    @AfterEach
    void releaseLock() {
        orchestrator.abort();
        turnService.invalidateTurnCache();
        Mockito.reset(movementService);
    }

    private static final class Scenario {
        final Board board;
        final Long attackerId;
        final Long defenderId;
        final int fromSector;
        final int toSector;
        final Long bruteId;

        Scenario(Board board, Long attackerId, Long defenderId,
                 int fromSector, int toSector, Long bruteId) {
            this.board = board;
            this.attackerId = attackerId;
            this.defenderId = defenderId;
            this.fromSector = fromSector;
            this.toSector = toSector;
            this.bruteId = bruteId;
        }
    }

    // Doit être appelée dans une méthode @Transactional (les tests le sont par défaut via la classe).
    private Scenario setupBruteVsLarbinOrder() {
        Board board = boardRepository.findAll().stream().findFirst().orElseThrow();
        List<Player> players = playerRepository.findAll();
        Player attacker = players.get(0);
        Player defender = players.stream()
                .filter(p -> !p.getId().equals(attacker.getId()))
                .findFirst().orElseThrow();
        Long attackerId = attacker.getId();
        Long defenderId = defender.getId();

        Sector s1 = board.getAllSectors().stream()
                .filter(s -> s.isNeutral() && s.getArmySize() == 0)
                .findFirst().orElseThrow(() -> new AssertionError("Aucun secteur neutre vide pour s1"));
        Sector s2 = board.getAllSectors().stream()
                .filter(s -> s.isNeutral() && s.getArmySize() == 0 && s.getNumber() != s1.getNumber())
                .findFirst().orElseThrow(() -> new AssertionError("Aucun secteur neutre vide pour s2"));
        s1.addNeighbor(s2.getNumber());
        s2.addNeighbor(s1.getNumber());

        Unit brute = new Unit(8.0, UnitClass.TIREUR);
        brute.setPlayerId(attackerId);
        s1.addUnit(brute);
        Unit larbin = new Unit(0.0, UnitClass.TIREUR);
        larbin.setPlayerId(defenderId);
        s2.addUnit(larbin);
        em.flush();
        Long bruteId = brute.getId();
        assertNotNull(bruteId);

        int turn = turnService.getCurrentTurn();
        orderRepository_deletePending(turn);
        MovementOrder order = MovementOrder.createFootOrder(
                attackerId, turn, List.of(bruteId), List.of(s1.getNumber(), s2.getNumber()));
        movementOrderRepository.save(order);
        em.flush();

        return new Scenario(board, attackerId, defenderId, s1.getNumber(), s2.getNumber(), bruteId);
    }

    private void orderRepository_deletePending(int turn) {
        List<MovementOrder> pending = movementOrderRepository.findPendingByTurn(turn);
        if (!pending.isEmpty()) {
            movementOrderRepository.deleteAll(pending);
            em.flush();
        }
    }

    @Nested
    @DisplayName("Démarrage de session")
    class Demarrage {

        @Test
        @Transactional
        @DisplayName("start : prépare les ordres, verrouille le tour et expose maxSteps")
        void start_prepareLesOrdresEtVerrouilleLeTour() {
            Scenario sc = setupBruteVsLarbinOrder();
            int turn = turnService.getCurrentTurn();

            TurnResolutionStateDto state = orchestrator.startSession();

            assertTrue(state.isActive());
            assertEquals(turn, state.getTurnEnding());
            assertEquals(0, state.getCurrentStep(), "Aucun hop effectué au démarrage");
            assertEquals(1, state.getMaxSteps(), "La route [s1, s2] fait 1 hop");
            assertTrue(state.isCanAdvance(), "On peut avancer au 1er hop");
        }

        @Test
        @Transactional
        @DisplayName("start : échoue (409) si une session est déjà active")
        void start_echoueSiUneSessionEstDejaActive() {
            setupBruteVsLarbinOrder();
            orchestrator.startSession();

            assertThrows(IllegalStateException.class, orchestrator::startSession,
                    "Un 2e start doit lever IllegalStateException (→ 409)");
        }

        @Test
        @DisplayName("getState sans session active renvoie active=false")
        void getStateSansSession_renvoieActiveFalse() {
            TurnResolutionStateDto state = orchestrator.getState();

            assertFalse(state.isActive());
        }
    }

    @Nested
    @DisplayName("Hop par hop")
    class HopParHop {

        @Test
        @Transactional
        @DisplayName("advanceHop : déplace les unités et expose les conflits du hop courant")
        void advanceHop_deplaceLesUnitesEtExposeLesConflits() {
            Scenario sc = setupBruteVsLarbinOrder();
            orchestrator.startSession();

            TurnResolutionStateDto state = orchestrator.advanceHop();

            assertEquals(1, state.getCurrentStep());
            assertEquals(1, state.getPendingConflicts().size(),
                    "Un conflit : le BRUTE arrive sur le LARBIN");
            PendingConflictDto pc = state.getPendingConflicts().get(0);
            assertEquals(sc.toSector, pc.getSectorNumber());
            assertEquals(sc.attackerId, pc.getAttackerPlayerId());
            assertEquals(sc.defenderId, pc.getDefenderPlayerId());
            assertFalse(state.isCanAdvance(), "Hop suivant bloqué tant qu'une bataille est en attente");
        }

        @Test
        @Transactional
        @DisplayName("advanceHop : refusé (409) tant que des conflits sont en attente")
        void advanceHop_refuseTantQueDesConflitsSontEnAttente() {
            setupBruteVsLarbinOrder();
            orchestrator.startSession();
            orchestrator.advanceHop();

            assertThrows(IllegalStateException.class, orchestrator::advanceHop,
                    "Avancer au hop suivant doit échouer tant qu'une bataille est en attente");
        }

        @Test
        @Transactional
        @DisplayName("advanceHop : refusé (409) après le dernier hop")
        void advanceHop_apresLeDernierHop_echoue() {
            setupBruteVsLarbinOrder();
            orchestrator.startSession();
            orchestrator.advanceHop();
            int conflictId = orchestrator.getState().getPendingConflicts().get(0).getConflictId();
            orchestrator.resolveBattle(conflictId);

            assertThrows(IllegalStateException.class, orchestrator::advanceHop,
                    "Avancer au-delà du dernier hop doit échouer (maxSteps atteint)");
        }

        @Test
        @Transactional
        @DisplayName("advanceHop : un ordre annulé entre deux hops ne déplace plus l'unité")
        void advanceHop_ordreAnnuleEntreHops_neDeplacePlusLUnite() {
            ScenarioSummaryDto seed = seeder.seedScenario();
            Long attackerId = seed.getAttacker().getId();
            Long attackerUnitId = seed.getAttackerUnit().getId();
            int turn = turnService.getCurrentTurn();

            orchestrator.startSession();
            orchestrator.advanceHop();

            // cancelOrderOrThrow ne prend pas le TurnLock : permet d'annuler entre deux hops.
            List<MovementOrder> pending = movementOrderRepository.findPendingByTurn(turn);
            assertFalse(pending.isEmpty(), "Prérequis : l'ordre du seeder est PENDING");
            movementService.cancelOrderOrThrow(attackerId, pending.get(0).getId());

            TurnResolutionStateDto state = orchestrator.advanceHop();
            assertEquals(2, state.getCurrentStep(), "Le hop 2 s'exécute (maxSteps atteint)");

            em.flush();
            em.clear();
            Board board = boardRepository.findAll().stream().findFirst().orElseThrow();
            boolean at13 = board.getSector(13).getUnits().stream()
                    .anyMatch(u -> attackerUnitId.equals(u.getId()));
            boolean at32 = board.getSector(32).getUnits().stream()
                    .anyMatch(u -> attackerUnitId.equals(u.getId()));
            assertTrue(at13, "L'unité annulée doit rester au secteur intermédiaire 13");
            assertFalse(at32, "L'unité annulée ne doit pas atteindre le secteur final 32");
        }
    }

    @Nested
    @DisplayName("Résolution d'une bataille")
    class ResolutionBataille {

        @Test
        @Transactional
        @DisplayName("resolveBattle : applique les pertes et persiste les survivants")
        void resolveBattle_appliqueLesPertesEtPersisteLesSurvivants() {
            Scenario sc = setupBruteVsLarbinOrder();
            orchestrator.startSession();
            orchestrator.advanceHop();
            int conflictId = orchestrator.getState().getPendingConflicts().get(0).getConflictId();

            ResolvedBattleDto report = orchestrator.resolveBattle(conflictId);

            assertTrue(report.isSuccess());
            assertEquals(sc.toSector, report.getSectorNumber());
            assertEquals(0, report.getAttackerCasualties(), "Le BRUTE survit");
            assertEquals(1, report.getDefenderCasualties(), "Le LARBIN est détruit");
            assertEquals(1, report.getAttackerInjured(), "Le BRUTE est blessé");

            em.flush();
            em.clear();
            Sector toReload = boardRepository.findAll().stream()
                    .findFirst().orElseThrow().getSector(sc.toSector);
            assertEquals(1, toReload.getArmySize());
            Unit survivor = toReload.getUnits().getFirst();
            assertTrue(survivor.isInjured());
            assertEquals(sc.attackerId, survivor.getPlayerId());
        }

        @Test
        @Transactional
        @DisplayName("resolveBattle : avec un conflictId introuvable → 404 (IllegalArgumentException)")
        void resolveBattle_avecIdIntrouvable_renvoie404() {
            setupBruteVsLarbinOrder();
            orchestrator.startSession();

            assertThrows(IllegalArgumentException.class, () -> orchestrator.resolveBattle(99999),
                    "Un conflictId inexistant doit lever IllegalArgumentException (→ 404)");
        }
    }

    @Nested
    @DisplayName("Finalisation du tour")
    class Finalisation {

        @Test
        @Transactional
        @DisplayName("finalizeTurn : incrémente le tour et libère le verrou")
        void finalizeTurn_incrementeLeTourEtLibereLeVerrou() {
            setupBruteVsLarbinOrder();
            int turnBefore = turnService.getCurrentTurn();
            orchestrator.startSession();
            orchestrator.advanceHop();
            int conflictId = orchestrator.getState().getPendingConflicts().get(0).getConflictId();
            orchestrator.resolveBattle(conflictId);

            TurnFinalizeResultDto result = orchestrator.finalizeTurn();

            assertEquals(turnBefore + 1, result.getNewTurn());
            assertTrue(result.getResolvedOrders() >= 1, "L'ordre attaquant est marqué résolu");
            assertFalse(orchestrator.getState().isActive(), "La session est fermée après finalize");
        }

        @Test
        @Transactional
        @DisplayName("finalizeTurn : refusé (409) tant que des hops restent")
        void finalizeTurn_tantQueDesHopsRestent_echoue() {
            setupBruteVsLarbinOrder();
            orchestrator.startSession();

            assertThrows(IllegalStateException.class, orchestrator::finalizeTurn,
                    "Finaliser sans avoir avancé tous les hops doit échouer");
        }

        @Test
        @Transactional
        @DisplayName("finalizeTurn : refusé (409) tant qu'une bataille est en attente")
        void finalizeTurn_tantQueDesBataillesSontEnAttente_echoue() {
            setupBruteVsLarbinOrder();
            orchestrator.startSession();
            orchestrator.advanceHop();

            assertThrows(IllegalStateException.class, orchestrator::finalizeTurn,
                    "Finaliser avec une bataille en attente doit échouer");
        }

        @Test
        @Transactional
        @DisplayName("finalizeTurn : invalide le cache du tour courant de TurnService")
        void finalizeTurn_invalideLeCacheDuTourCourant() {
            setupBruteVsLarbinOrder();
            int turnBefore = turnService.getCurrentTurn();
            orchestrator.startSession();
            orchestrator.advanceHop();
            int conflictId = orchestrator.getState().getPendingConflicts().get(0).getConflictId();
            orchestrator.resolveBattle(conflictId);

            orchestrator.finalizeTurn();

            assertEquals(turnBefore + 1, turnService.getCurrentTurn(),
                    "Le cache du tour doit être invalidé après finalizeTurn "
                            + "→ getCurrentTurn retourne le nouveau tour");
        }

        @Test
        @Transactional
        @DisplayName("finalizeTurn : libère le verrou même sur exception après les gardes")
        void finalizeTurn_libereLeVerrouSurExceptionApresLesGardes() {
            setupBruteVsLarbinOrder();
            orchestrator.startSession();
            orchestrator.advanceHop();
            int conflictId = orchestrator.getState().getPendingConflicts().get(0).getConflictId();
            orchestrator.resolveBattle(conflictId);

            // Simule une erreur DB après les gardes : avant le fix, le verrou restait acquis.
            Mockito.doThrow(new RuntimeException("Simulated DB error"))
                    .when(movementService).finalizeResolution(any(), any());

            assertThrows(RuntimeException.class, orchestrator::finalizeTurn,
                    "finalizeTurn doit propager l'exception");

            assertFalse(turnLock.isLocked(),
                    "Le verrou doit être libéré même sur exception (try/finally)");
            assertFalse(orchestrator.getState().isActive(),
                    "La session doit être fermée même sur exception (try/finally)");
        }
    }

    @Nested
    @DisplayName("Abandon de session")
    class Abandon {

        @Test
        @Transactional
        @DisplayName("abort : libère le verrou sans rollback des positions déjà déplacées")
        void abort_libereLeVerrouSansRollbackDesPositions() {
            Scenario sc = setupBruteVsLarbinOrder();
            orchestrator.startSession();
            orchestrator.advanceHop();

            orchestrator.abort();

            assertFalse(orchestrator.getState().isActive(), "Session fermée");
            // abort soft : pas de rollback des positions déjà déplacées (vérifié après reload DB).
            em.flush();
            em.clear();
            Sector toReload = boardRepository.findAll().stream()
                    .findFirst().orElseThrow().getSector(sc.toSector);
            assertTrue(toReload.getUnits().stream().anyMatch(u -> sc.bruteId.equals(u.getId())),
                    "L'unité attaquante doit rester au secteur de destination après abort (pas de rollback)");
        }

        @Test
        @DisplayName("abort sans session active est idempotent (no-op)")
        void abort_sansSessionActive_estIdempotent() {
            assertDoesNotThrow(() -> orchestrator.abort());
        }
    }
}
