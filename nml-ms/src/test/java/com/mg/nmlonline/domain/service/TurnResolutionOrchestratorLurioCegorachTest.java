package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.PendingConflictDto;
import com.mg.nmlonline.api.dto.ResolvedBattleDto;
import com.mg.nmlonline.api.dto.ScenarioSummaryDto;
import com.mg.nmlonline.api.dto.TurnResolutionStateDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.building.Building;
import com.mg.nmlonline.domain.model.building.BuildingType;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Régression Phase 3 : unité équipée (unitEquipments LAZY) MOVED 2 hops puis détruite doit cascade-DELETE unit_equipments (avant : release-FK-NULL → unit_id NOT NULL → 500).
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DisplayName("TurnResolutionOrchestrator — scénario Lurio → Cegorach (2 hops)")
class TurnResolutionOrchestratorLurioCegorachTest {

    @Autowired
    private TurnResolutionScenarioSeeder seeder;

    @Autowired
    private TurnResolutionOrchestrator orchestrator;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private PlatformTransactionManager txManager;

    @AfterEach
    void releaseLock() {
        // libère le verrou si une assertion échoue avant abort/finalize (sinon 409 sur les tests suivants).
        orchestrator.abort();
    }

    @Test
    @DisplayName("seed + 2 hops : l'unité LEGER atteint le secteur 32 et émet un PendingConflict")
    @Transactional
    void lurioVsCegorach_deuxHops_deplacent_attaquant_vers_32_et_cree_conflit() {
        ScenarioSummaryDto seed = seeder.seedScenario();
        em.flush();
        Long lurioId = seed.getAttacker().getId();
        Long cegorachId = seed.getDefender().getId();
        Long attackerUnitId = seed.getAttackerUnit().getId();
        assertNotNull(attackerUnitId, "Le seeder doit identifier l'unité attaquante");

        TurnResolutionStateDto state = orchestrator.startSession();
        assertTrue(state.isActive(), "La session doit être active après start");
        assertEquals(0, state.getCurrentStep(), "Aucun hop après start");
        assertEquals(2, state.getMaxSteps(), "La route [41, 13, 32] fait 2 hops");
        assertTrue(state.isCanAdvance(), "On peut avancer tant qu'aucun conflit n'est en attente");

        state = orchestrator.advanceHop();
        assertEquals(1, state.getCurrentStep());
        assertTrue(state.getPendingConflicts().isEmpty(),
                "Secteur 13 = lurio : arrivée sans ennemi → pas de conflit");

        // Le fixture lurio.json met 6 unités en secteur 41 ; seule l'attaquante bouge.
        em.flush();
        Board board = boardRepository.findAll().stream().findFirst().orElseThrow();
        assertEquals(0, board.getSector(41).getUnits().stream()
                        .filter(u -> attackerUnitId.equals(u.getId())).count(),
                "L'unité attaquante a quitté le secteur 41 après le hop 1");
        assertEquals(1, board.getSector(13).getUnits().stream()
                        .filter(u -> attackerUnitId.equals(u.getId())).count(),
                "L'unité attaquante est arrivée en secteur 13 après le hop 1");

        state = orchestrator.advanceHop();
        assertEquals(2, state.getCurrentStep(), "maxSteps atteint");
        assertEquals(1, state.getPendingConflicts().size(),
                "Un conflit attendu : l'attaquant arrive sur cegorach en 32");
        PendingConflictDto pc = state.getPendingConflicts().getFirst();
        assertEquals(32, pc.getSectorNumber());
        assertEquals(lurioId, pc.getAttackerPlayerId());
        assertEquals(cegorachId, pc.getDefenderPlayerId());
        assertFalse(state.isCanAdvance(), "Hop suivant bloqué tant que la bataille est en attente");
    }

    @Test
    @DisplayName("resolveBattle sur l'attaquant équipé MOVED détruit proprement (régression du fix Phase 3)")
    void lurioVsCegorach_resolveBattle_surAttaquantEquipeDeplace_detruitProprementPhase3() {
        ScenarioSummaryDto seed = seeder.seedScenario();
        Long lurioId = seed.getAttacker().getId();
        Long cegorachId = seed.getDefender().getId();
        Long attackerUnitId = seed.getAttackerUnit().getId();
        assertNotNull(attackerUnitId);
        orchestrator.startSession();
        orchestrator.advanceHop();
        TurnResolutionStateDto state = orchestrator.advanceHop();
        assertEquals(1, state.getPendingConflicts().size(),
                "Prérequis : un conflit émis en secteur 32 avant resolveBattle");
        int conflictId = state.getPendingConflicts().getFirst().getConflictId();

        ResolvedBattleDto report = orchestrator.resolveBattle(conflictId);
        assertTrue(report.isSuccess(), "Le combat se déroule (les deux camps sont présents)");
        assertEquals(32, report.getSectorNumber());
        assertEquals(1, report.getAttackerCasualties(),
                "L'unique attaquant LEGER est détruit (force écrasante des 2 BRUTEs 100/100)");
        assertEquals(0, report.getDefenderCasualties(),
                "Les 2 BRUTE 100/100 ne subissent aucune perte face à un LEGER attaquant");
        assertEquals(0, report.getAttackerInjured(), "L'attaquant meurt, pas de blessé");
        // Le VOYOU équipé (HK-MP7 : pdf 60) entame le BRUTE n°2 en phase PDF (def 100 → 40),
        // puis meurt en phase bâtiments secondaires (Cache 100 + Banque 50 vs 20 def + 10 armure).
        assertEquals(1, report.getDefenderInjured(),
                "Le BRUTE n°2 (def 40 < 100 après le pdf de l'attaquant) termine blessé");
        assertEquals(cegorachId, report.getWinnerId(),
                "Attaquant anéanti ⇒ le défenseur garde le secteur (règle §2 du plan combat)");
        assertEquals(0, report.getCapturedBuildings(), "Pas de capture : le défenseur a gagné");
        assertFalse(report.isDefenderCharacterLost(),
                "Le personnage de cegorach est au secteur 1, il ne participe pas");

        // Pas de @Transactional sur ce test : un @Transactional de test masquerait la DataIntegrityViolationException au commit (docs/jpa-pitfalls.md §2).
        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            Sector secteur32 = boardRepository.findAll().stream()
                    .findFirst().orElseThrow().getSector(32);
            long lurioRestant = secteur32.getUnits().stream()
                    .filter(u -> lurioId.equals(u.getPlayerId())).count();
            long cegorachRestant = secteur32.getUnits().stream()
                    .filter(u -> cegorachId.equals(u.getPlayerId())).count();
            assertEquals(0, lurioRestant,
                    "L'attaquant détruit est retiré du secteur 32 (em.remove explicite en Phase 3)");
            assertEquals(2, cegorachRestant, "Les 2 BRUTEs défenseurs survivent en secteur 32");

            // Les 3 bâtiments de cegorach ont participé (cible = unités d'abord) : intacts et régénérés.
            List<Building> batiments = secteur32.getBuildings().stream()
                    .filter(b -> cegorachId.equals(b.getPlayerId())).toList();
            assertEquals(3, batiments.size(), "QG + Cache + Banque restent en secteur 32");
            assertTrue(batiments.stream().noneMatch(Building::isDestroyed));
            Building qg = batiments.stream()
                    .filter(b -> b.getBuildingType() == BuildingType.HEADQUARTERS).findFirst().orElseThrow();
            assertEquals(200.0, qg.getDefense(), "PV régénérés : le QG revient à 200 après le reassign-zéro");
            assertEquals(100.0, qg.getAttack());

            long ueCount = em.createQuery(
                            "select count(ue) from UnitEquipment ue where ue.unit.id = :unitId", Long.class)
                    .setParameter("unitId", attackerUnitId)
                    .getSingleResult();
            assertEquals(0, ueCount,
                    "Les rows unit_equipments de l'attaquant détruit doivent être effacées par la cascade"
                            + " (em.remove → Unit.unitEquipments cascade=ALL → DELETE unit_equipments)");
        });
    }
}