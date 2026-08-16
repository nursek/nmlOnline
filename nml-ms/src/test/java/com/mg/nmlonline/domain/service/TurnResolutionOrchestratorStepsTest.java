package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.PendingConflictDto;
import com.mg.nmlonline.api.dto.ResolvedBattleDto;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests focaux de la résolution pas-à-pas par hop, organisés par étape du
 * cycle ({@link Nested @Nested} classes) pour servir de <strong>documentation
 * vivante</strong> de la feature en français.
 *
 * <p>Chaque scénario est déterministe (pas d'évasion → pas de RNG) : un BRUTE
 * (100/100, expérience 8) attaquant vs un LARBIN (10/10, expérience 0)
 * défenseur, sur deux secteurs neutres voisins du seed. Le BRUTE écrase le
 * LARBIN et survit blessé (stats ÷ 2).</p>
 *
 * <p>{@code @AfterEach} appelle {@code abort()} pour libérer le verrou
 * {@link TurnLock} si une assertion a échoué avant finalize/abort (sinon les
 * tests suivants bloqueraient sur le 409).</p>
 *
 * <p>Le gros E2E monolithique vit dans {@link TurnResolutionOrchestratorTest};
 * cette classe détaille chaque étape et les cas aux limites (409, 404,
 * idempotence).</p>
 */
@SpringBootTest
@ActiveProfiles("test")
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

    @AfterEach
    void releaseLock() {
        orchestrator.abort();
    }

    // ==================================================
    // === Helper : scénario brute vs larbin, 1 hop =====
    // ==================================================

    /** État initial du scénario : 2 secteurs voisins, ordre PENDING 1 hop prêt. */
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

    /**
     * Prépare un scénario brute-vs-larbin 1 hop pour le tour courant. Recherche
     * 2 secteurs neutres vides voisins dans le seed, y place les unités, crée
     * l'ordre PENDING, flushe. Doit être appelée dans une méthode
     * {@code @Transactional} (les tests le sont par défaut via la classe).
     */
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
        // Rendre les deux secteurs voisins pour que la route [s1, s2] soit valide.
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

    // ==================================================
    // === 1. Démarrage de session ======================
    // ==================================================

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

    // ==================================================
    // === 2. Hop par hop ===============================
    // ==================================================

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
            // Résoudre la bataille pour débloquer le suivant.
            int conflictId = orchestrator.getState().getPendingConflicts().get(0).getConflictId();
            orchestrator.resolveBattle(conflictId);

            assertThrows(IllegalStateException.class, orchestrator::advanceHop,
                    "Avancer au-delà du dernier hop doit échouer (maxSteps atteint)");
        }
    }

    // ==================================================
    // === 3. Résolution d'une bataille =================
    // ==================================================

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

            // Persistance : seul le BRUTE (blessé) reste au secteur de destination.
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

    // ==================================================
    // === 4. Finalisation ==============================
    // ==================================================

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
            // Aucun hop effectué.

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
            // Bataille non résolue.

            assertThrows(IllegalStateException.class, orchestrator::finalizeTurn,
                    "Finaliser avec une bataille en attente doit échouer");
        }
    }

    // ==================================================
    // === 5. Abandon ===================================
    // ==================================================

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
            // L'unité attaquante, déjà déplacée au hop 1, reste au secteur de destination
            // (abort soft — pas de rollback des positions). On recharge depuis la base
            // pour s'assurer qu'il s'agit bien d'un état persistant, pas seulement mémoire.
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