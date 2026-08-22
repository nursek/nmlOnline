package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.PendingConflictDto;
import com.mg.nmlonline.api.dto.ResolvedBattleDto;
import com.mg.nmlonline.api.dto.ScenarioSummaryDto;
import com.mg.nmlonline.api.dto.TurnResolutionStateDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Caractérise le scénario pédagogique Lurio → Cegorach traversant l'orchestrateur
 * pas-à-pas hop par hop.
 *
 * <h3>Test 1 — happy path</h3>
 * {@code seedScenario} → {@code startSession} → {@code advanceHop} (×2) déplacent
 * effectivement l'unité attaquante LEGER du secteur 41 au secteur 32 et y émettent
 * un {@code PendingConflict(lurio, cegorach)}. Cette traçabilité invalide le no-op
 * « Unités manquantes au secteur 32 (attaquant=0) » observé en UI quand
 * {@code resolveBattle} est invoqué AVANT les hops.
 *
 * <h3>Test 2 — régression du fix Phase 3</h3>
 * L'unité attaquante est le premier LEGER du fixture {@code players/lurio.json}
 * (au tri actuel de {@code sortArmy} : le VOYOU id 7, exp 2 — voir note ci-dessous),
 * portant deux {@code UnitEquipment} (HK-MP7 + Tenue ultra légère) chargés en LAZY.
 * DÉPLACÉE sur 2 hops (41 → 13 → 32) puis déclarée pertes de combat à
 * {@code resolveBattle}, sa destruction doit cascader proprement à ses rows
 * {@code unit_equipments} (DELETE via {@code Unit.unitEquipments} cascade=ALL).
 *
 * <p>Avant le fix Phase 3, ce chemin échouait à l'auto-flush
 * ({@code resolveNames} → {@code findAllById}) par
 * <pre>update unit_equipments set equipment_id=?, unit_id=NULL where id=?</pre>
 * (release-FK Hibernate 6.6.29 émise depuis l'orphanRemoval de {@code Sector.army}
 * sur la collection inverse-side {@code Unit.unitEquipments} LAZY non initialisée
 * d'une Unit ayant MOVED entre 2 collections orphanRemoval) qui heurtait
 * {@code unit_id NOT NULL} → {@code DataIntegrityViolationException}. Variante
 * du piège {@code docs/jpa-pitfalls.md} §1 NON couverte par le fix Phase 2
 * (ce dernier retirait orphanRemoval de {@code Unit.unitEquipments} et couvrait
 * « équipé non-déplacé + unitEquipments créé en mémoire puis flushed →
 * PersistentBag initialisé » via
 * {@code TurnResolutionOrchestratorTest#resolveBattle_perteUniteEquipee_...},
 * mais pas « équipé chargé LAZY de DB + MOVED 2 hops + pertes »).
 *
 * <p>Le fix Phase 3 retire à son tour {@code orphanRemoval=true} de
 * {@code Sector.army} ({@link com.mg.nmlonline.domain.model.sector.Sector} +
 * {@code V4__sector_army_fk_cascade.sql} FK ON DELETE CASCADE) :
 * <ul>
 *   <li>{@code CombatService.simulateSectorBattle} lève désormais
 *       {@code em.remove(unit)} explicite pour chaque pertes (cascade REMOVE →
 *       DELETE unit_equipments + DELETE combat_entities propre, pas d'UPDATE
 *       release-FK-NULL) ;</li>
 *   <li>{@code SectorService.removePlayerFromSectors} lève {@code em.remove(unit)}
 *       avant le {@code .clear()} pour préserver la DELETE des armées d'un joueur
 *       supprimé ;</li>
 *   <li>{@code MovementService.advanceOrder} reste inchangé : la FK du secteur
 *       est pilotée côté owning par {@code entity.setSector(target)} (UPDATE
 *       combat_entities SET sector_number), pas par l'orphanRemoval — donc pas
 *       d'impact.</li>
 * </ul>
 *
 * <p>Données seedées par {@code PlayerStartupImporter} au profil {@code test}
 * (H2) : {@code players/lurio.json} (secteurs 13 et 41) +
 * {@code players/cegorach.json} (secteurs 27 et 32, army vide) +
 * {@code boards/board.json} (route 41 ↔ 13 ↔ 32 valide). La transaction de test
 * annule les effets en fin de méthode ; {@code @AfterEach} libère le verrou de
 * fin de tour au cas où.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
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

    @AfterEach
    void releaseLock() {
        // Sécurité : libère le verrou si une assertion a échoué avant finalizeTurn/abort.
        orchestrator.abort();
    }

    @Test
    @DisplayName("seed + 2 hops : l'unité LEGER atteint le secteur 32 et émet un PendingConflict")
    @Transactional
    void lurioVsCegorach_deuxHops_deplacent_attaquant_vers_32_et_cree_conflit() {
        // === seed : ordre PENDING route [41, 13, 32] + 2 BRUTEs défenseurs en 32 ===
        ScenarioSummaryDto seed = seeder.seedScenario();
        em.flush();
        Long lurioId = seed.getAttacker().getId();
        Long cegorachId = seed.getDefender().getId();
        Long attackerUnitId = seed.getAttackerUnit().getId();
        assertNotNull(attackerUnitId, "Le seeder doit identifier l'unité attaquante");

        // === start : currentStep = 0, maxSteps = 2 (route [41, 13, 32]) ===
        TurnResolutionStateDto state = orchestrator.startSession();
        assertTrue(state.isActive(), "La session doit être active après start");
        assertEquals(0, state.getCurrentStep(), "Aucun hop après start");
        assertEquals(2, state.getMaxSteps(), "La route [41, 13, 32] fait 2 hops");
        assertTrue(state.isCanAdvance(), "On peut avancer tant qu'aucun conflit n'est en attente");

        // === hop 1 (41 → 13) : secteur allié lurio → aucun conflit ===
        state = orchestrator.advanceHop();
        assertEquals(1, state.getCurrentStep());
        assertTrue(state.getPendingConflicts().isEmpty(),
                "Secteur 13 = lurio : arrivée sans ennemi → pas de conflit");

        // L'unique unité attaquante (et elle seule) a quitté 41 pour 13. Le fixture
        // lurio.json met 6 unités en secteur 41 — les 5 autres y restent.
        em.flush();
        Board board = boardRepository.findAll().stream().findFirst().orElseThrow();
        assertEquals(0, board.getSector(41).getUnits().stream()
                        .filter(u -> attackerUnitId.equals(u.getId())).count(),
                "L'unité attaquante a quitté le secteur 41 après le hop 1");
        assertEquals(1, board.getSector(13).getUnits().stream()
                        .filter(u -> attackerUnitId.equals(u.getId())).count(),
                "L'unité attaquante est arrivée en secteur 13 après le hop 1");

        // === hop 2 (13 → 32) : secteur cegorach défendu → conflit émis ===
        state = orchestrator.advanceHop();
        assertEquals(2, state.getCurrentStep(), "maxSteps atteint");
        assertEquals(1, state.getPendingConflicts().size(),
                "Un conflit attendu : l'attaquant arrive sur cegorach en 32");
        PendingConflictDto pc = state.getPendingConflicts().getFirst();
        assertEquals(32, pc.getSectorNumber());
        assertEquals(lurioId, pc.getAttackerPlayerId());
        assertEquals(cegorachId, pc.getDefenderPlayerId());
        assertFalse(state.isCanAdvance(), "Hop suivant bloqué tant que la bataille est en attente");
        // Ce test s'arrête AVANT resolveBattle (le test 2 character règle le déroulé).
    }

    @Test
    @DisplayName("resolveBattle sur l'attaquant équipé MOVED détruit proprement (régression du fix Phase 3)")
    @Transactional
    void lurioVsCegorach_resolveBattle_surAttaquantEquipeDeplace_detruitProprementPhase3() {
        // Setup identique au test vert jusqu'au conflit : seed → start → hop1 → hop2.
        ScenarioSummaryDto seed = seeder.seedScenario();
        Long lurioId = seed.getAttacker().getId();
        Long cegorachId = seed.getDefender().getId();
        Long attackerUnitId = seed.getAttackerUnit().getId();
        assertNotNull(attackerUnitId);
        orchestrator.startSession();
        orchestrator.advanceHop(); // 41 → 13
        TurnResolutionStateDto state = orchestrator.advanceHop(); // 13 → 32 → conflit
        assertEquals(1, state.getPendingConflicts().size(),
                "Prérequis : un conflit émis en secteur 32 avant resolveBattle");
        int conflictId = state.getPendingConflicts().getFirst().getConflictId();

        // === resolveBattle : pertes = attaquant LEGER équipé (HK-MP7 + Tenue ultra
        // légère) chargé LAZY, MOVED 2 hops, puis détruit au combat. Avant le fix
        // Phase 3, ce chemin échouait à l'auto-flush (resolveNames → findAllById) par
        //   update unit_equipments set equipment_id=?, unit_id=NULL where id=?
        // (release-FK Hibernate depuis l'orphanRemoval Sector.army) qui heurtait
        // unit_id NOT NULL. Phase 3 retire orphanRemoval de Sector.army (mapping +
        // Flyway V4 ON DELETE CASCADE) et déplace la DELETE explicite vers
        // CombatService.simulateSectorBattle (em.remove cascadé). ===
        // ponytail: le type exact (VOYOU vs LARBIN) dépend du sortArmy + fixture et
        // n'est pas asserté — la valeur du test est la régression cascade, pas le
        // type. Au tri actuel, pickAttacker retourne le VOYOU id 7 (exp 2 > LARBINs).
        ResolvedBattleDto report = orchestrator.resolveBattle(conflictId);
        assertTrue(report.isSuccess(), "Le combat se déroule (les deux camps sont présents)");
        assertEquals(32, report.getSectorNumber());
        assertEquals(1, report.getAttackerCasualties(),
                "L'unique attaquant LEGER est détruit (force écrasante des 2 BRUTEs 100/100)");
        assertEquals(0, report.getDefenderCasualties(),
                "Les 2 BRUTE 100/100 ne subissent aucune perte face à un LEGER attaquant");
        assertEquals(0, report.getAttackerInjured(), "L'attaquant meurt, pas de blessé");
        // comportement actuel : Battle.classicPhaseConfiguration cible toujours le LAST
        // unit du defender list (defender.getLast()), donc l'attaquant concentre tout
        // son feu sur le BRUTE n°2. Sa defense passe sous baseDefense → replaceWithInjured
        // (Battle.java:122) le marque blessé (stats ÷ 2). Déterministe car le BRUTE a
        // evasion=0 (pas de RNG).
        assertEquals(1, report.getDefenderInjured(),
                "Le BRUTE n°2 ciblé par l'attaquant est blessé (defense tombée sous baseDefense 100)");
        // comportement actuel piné — à revoir : Battle.classicCombatConfiguration
        // ne set jamais this.winner → ResolvedBattle.winnerId est null bien qu'ici le
        // défenseur soit trivialement vainqueur. À corriger dans Battle.
        assertNull(report.getWinnerId(),
                "Battle ne set jamais winner — à revoir (cf. GameSimulationIT.shouldResolveDeterministicBattleOutcome)");

        // === persistance : l'attaquant est retiré du secteur 32, les BRUTEs y restent ===
        em.flush();
        em.clear();
        Sector secteur32 = boardRepository.findAll().stream()
                .findFirst().orElseThrow().getSector(32);
        long lurioRestant = secteur32.getUnits().stream()
                .filter(u -> lurioId.equals(u.getPlayerId())).count();
        long cegorachRestant = secteur32.getUnits().stream()
                .filter(u -> cegorachId.equals(u.getPlayerId())).count();
        assertEquals(0, lurioRestant,
                "L'attaquant détruit est retiré du secteur 32 (em.remove explicite en Phase 3)");
        assertEquals(2, cegorachRestant, "Les 2 BRUTEs défenseurs survivent en secteur 32");

        // === régression clé : les rows unit_equipments de l'attaquant détruit sont
        // effacées par la cascade Hibernate REMOVE → DELETE (preuve que le fix Phase 3
        // déroule le nettoyage proprement, sans la release-FK-NULL d'avant). ===
        long ueCount = em.createQuery(
                        "select count(ue) from UnitEquipment ue where ue.unit.id = :unitId", Long.class)
                .setParameter("unitId", attackerUnitId)
                .getSingleResult();
        assertEquals(0, ueCount,
                "Les rows unit_equipments de l'attaquant détruit doivent être effacées par la cascade"
                        + " (em.remove → Unit.unitEquipments cascade=ALL → DELETE unit_equipments)");
    }
}