package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.PendingConflictDto;
import com.mg.nmlonline.api.dto.ResolvedBattleDto;
import com.mg.nmlonline.api.dto.TurnFinalizeResultDto;
import com.mg.nmlonline.api.dto.TurnResolutionStateDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentCategory;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test d'intégration de la résolution pas-à-pas par hop
 * ({@link TurnResolutionOrchestrator}) : couvre le cycle complet
 * start → advanceHop → resolveBattle → finalizeTurn, y compris la persistance
 * réelle des pertes (orphanRemoval sur {@code Sector.army}).
 *
 * <p>Scénario déterministe (sans évasion, donc sans RNG) : un BRUTE (100/100,
 * expérience 8) attaquant vs un LARBIN (10/10, expérience 0) défenseur. Le
 * LARBIN est détruit en phase ATK, le BRUTE survit blessé (stats ÷ 2).</p>
 *
 * <p>Données seedées par {@code PlayerStartupImporter} au profil {@code test}
 * (H2). Deux secteurs neutres vides accueillent les unités du scénario pour ne
 * pas perturber le seed. La transaction de test annule les effets en fin de
 * méthode ; {@code @AfterEach} libère le verrou de fin de tour au cas où.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("TurnResolutionOrchestrator — pas-à-pas hop par hop")
class TurnResolutionOrchestratorTest {

    @Autowired
    private TurnResolutionOrchestrator orchestrator;

    @Autowired
    private TurnService turnService;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private MovementOrderRepository orderRepository;

    @Autowired
    private EntityManager em;

    @AfterEach
    void releaseLock() {
        // Sécurité : libère le verrou si une assertion a échoué avant finalizeTurn.
        orchestrator.abort();
    }

    @Test
    @DisplayName("un ordre adverse crée un conflit résolu manuellement puis le tour s'incrémente")
    @Transactional
    void conflictResolvedManuallyThenFinalize() {
        Board board = boardRepository.findAll().stream().findFirst().orElseThrow();
        List<Player> players = playerRepository.findAll();
        assertFalse(players.size() < 2, "Le seed doit fournir au moins 2 joueurs");
        Player attacker = players.get(0);
        Player defender = players.stream()
                .filter(p -> !p.getId().equals(attacker.getId()))
                .findFirst().orElseThrow();
        Long attackerId = attacker.getId();
        Long defenderId = defender.getId();

        // Deux secteurs neutres vides pour un scénario isolé (sans perturber le seed).
        Sector s1 = board.getAllSectors().stream()
                .filter(s -> s.isNeutral() && s.getArmySize() == 0)
                .findFirst().orElseThrow(() -> new AssertionError("Aucun secteur neutre vide pour s1"));
        Sector s2 = board.getAllSectors().stream()
                .filter(s -> s.isNeutral() && s.getArmySize() == 0 && s.getNumber() != s1.getNumber())
                .findFirst().orElseThrow(() -> new AssertionError("Aucun secteur neutre vide pour s2"));

        // BRUTE attaquant (100/100, pas d'évasion → déterministe) + LARBIN défenseur (10/10).
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
        // On s'assure qu'aucun ordre PENDING parasite de ce tour n'interfère.
        orderRepository.deleteAll(orderRepository.findPendingByTurn(turn));
        em.flush();

        MovementOrder order = MovementOrder.createFootOrder(
                attackerId, turn, List.of(bruteId), List.of(s1.getNumber(), s2.getNumber()));
        orderRepository.save(order);
        em.flush();

        // === start ===
        TurnResolutionStateDto state = orchestrator.startSession();
        assertTrue(state.isActive());
        assertEquals(turn, state.getTurnEnding());
        assertEquals(0, state.getCurrentStep());
        assertEquals(1, state.getMaxSteps(), "La route [s1, s2] fait exactement 1 hop");
        assertTrue(state.isCanAdvance());
        assertFalse(state.isCanFinalize());

        // === hop 1 : le BRUTE arrive sur s2 défendu par le LARBIN ===
        state = orchestrator.advanceHop();
        assertEquals(1, state.getCurrentStep());
        assertEquals(1, state.getPendingConflicts().size(),
                "Un conflit attendu : l'attaquant arrive sur le défenseur");
        PendingConflictDto pc = state.getPendingConflicts().get(0);
        assertEquals(s2.getNumber(), pc.getSectorNumber());
        assertEquals(attackerId, pc.getAttackerPlayerId());
        assertEquals(defenderId, pc.getDefenderPlayerId());
        assertFalse(state.isCanAdvance(), "Hop suivant bloqué tant qu'une bataille est en attente");

        // === resolve battle : BRUTE écrase le LARBIN, survit blessé ===
        ResolvedBattleDto report = orchestrator.resolveBattle(pc.getConflictId());
        assertTrue(report.isSuccess());
        assertEquals(s2.getNumber(), report.getSectorNumber());
        assertEquals(0, report.getAttackerCasualties(), "Le BRUTE survit");
        assertEquals(1, report.getDefenderCasualties(), "Le LARBIN est détruit");
        assertEquals(1, report.getAttackerInjured(), "Le BRUTE est blessé (defense 90 < 100)");
        assertEquals(0, report.getDefenderInjured());

        state = orchestrator.getState();
        assertTrue(state.getPendingConflicts().isEmpty());
        assertTrue(state.isCanFinalize());

        // === persistance des pertes : le LARBIN est retiré du secteur, le BRUTE (blessé) y reste ===
        em.flush();
        em.clear();
        Sector s2Refreshed = boardRepository.findAll().stream()
                .findFirst().orElseThrow().getSector(s2.getNumber());
        assertEquals(1, s2Refreshed.getArmySize(), "Seul le BRUTE (survivants) reste en s2");
        Unit survivor = s2Refreshed.getUnits().getFirst();
        assertTrue(survivor.isInjured(), "Le BRUTE doit être blessé après le combat");
        assertEquals(attackerId, survivor.getPlayerId());

        // === finalize : le tour s'incrémente ===
        TurnFinalizeResultDto fin = orchestrator.finalizeTurn();
        assertEquals(turn + 1, fin.getNewTurn());
        assertTrue(fin.getResolvedOrders() >= 1, "L'ordre de l'attaquant doit être résolu");

        // La session est fermée.
        assertFalse(orchestrator.getState().isActive());
    }

    @Test
    @DisplayName("resolveBattle détruit un défenseur équipé : la FK cascade efface ses rows unit_equipments (Phase 2)")
    @Transactional
    void resolveBattle_perteUniteEquipee_effaceRowsUnitEquipmentsViaFkCascade() {
        // Mécanisme de cascade sur pertes de combat (Phase 2 + Phase 3) :
        //   - Phase 2 : Unit.unitEquipments perd orphanRemoval (reste cascade=ALL +
        //     @OnDelete(CASCADE) + Flyway V3 FK ON DELETE CASCADE).
        //   - Phase 3 : Sector.army perd aussi orphanRemoval (Flyway V4). La DELETE
        //     des pertes se fait désormais par em.remove(unit) explicite dans
        //     CombatService.simulateSectorBattle → cascade REMOVE sur
        //     Unit.unitEquipments (cascade=ALL) → DELETE unit_equipments propre,
        //     sans UPDATE release-FK-NULL. La FK ON DELETE CASCADE (V3) est la
        //     ceinture DB-side au cas où l'ORM laisse un row orphelin.
        // Ce test couvre le variant « défenseur équipé créé en mémoire puis
        // persisté » (PersistentBag initialisé). Le variant « équipé chargé LAZY
        // de DB + MOVED 2 hops + pertes » (la variante piégée non couverte par
        // Phase 2) est caractérisé par
        // TurnResolutionOrchestratorLurioCegorachTest#lurioVsCegorach_resolveBattle_...
        Board board = boardRepository.findAll().stream().findFirst().orElseThrow();
        List<Player> players = playerRepository.findAll();
        Player attacker = players.get(0);
        Player defender = players.stream()
                .filter(p -> !p.getId().equals(attacker.getId())).findFirst().orElseThrow();

        Sector s1 = board.getAllSectors().stream()
                .filter(s -> s.isNeutral() && s.getArmySize() == 0).findFirst().orElseThrow();
        Sector s2 = board.getAllSectors().stream()
                .filter(s -> s.isNeutral() && s.getArmySize() == 0 && s.getNumber() != s1.getNumber())
                .findFirst().orElseThrow();

        Unit brute = new Unit(8.0, UnitClass.TIREUR);
        brute.setPlayerId(attacker.getId());
        s1.addUnit(brute);

        Unit larbin = new Unit(0.0, UnitClass.TIREUR);
        larbin.setPlayerId(defender.getId());
        Equipment gun = new Equipment("Pistolet cascade fk", 100, 10, 0, 0, 0,
                Set.of(UnitClass.TIREUR), EquipmentCategory.FIREARM);
        em.persist(gun);
        em.flush();
        larbin.addEquipment(gun);
        s2.addUnit(larbin);

        em.flush();
        Long larbinId = larbin.getId();
        assertNotNull(larbinId);
        assertFalse(larbin.getUnitEquipments().isEmpty(),
                "prérequis : le défenseur a une ligne unit_equipments persistée");
        Long ueId = larbin.getUnitEquipments().get(0).getId();
        assertNotNull(ueId);

        int turn = turnService.getCurrentTurn();
        orderRepository.deleteAll(orderRepository.findPendingByTurn(turn));
        em.flush();

        MovementOrder order = MovementOrder.createFootOrder(
                attacker.getId(), turn, List.of(brute.getId()),
                List.of(s1.getNumber(), s2.getNumber()));
        orderRepository.save(order);
        em.flush();

        orchestrator.startSession();
        TurnResolutionStateDto state = orchestrator.advanceHop();
        int conflictId = state.getPendingConflicts().get(0).getConflictId();
        ResolvedBattleDto report = orchestrator.resolveBattle(conflictId);

        assertTrue(report.isSuccess());
        assertEquals(1, report.getDefenderCasualties(),
                "Le défenseur équipé est détruit par le BRUTE 100/100");

        em.flush();
        em.clear();

        // La row unit_equipments doit être effacée : em.remove(unit) (Phase 3)
        // cascade REMOVE → Unit.unitEquipments (cascade=ALL) → DELETE unit_equipments.
        // La FK ON DELETE CASCADE (V3) est la ceinture DB si l'ORM laisse un orphelin.
        long ueCount = em.createQuery(
                        "select count(ue) from UnitEquipment ue where ue.id = :id", Long.class)
                .setParameter("id", ueId).getSingleResult();
        assertEquals(0, ueCount,
                "la row unit_equipments doit être effacée par la cascade REMOVE quand la Unit est DELETEd");
    }
}