package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.EmbeddedPostgresTest;
import com.mg.nmlonline.api.dto.PendingConflictDto;
import com.mg.nmlonline.api.dto.ResolvedBattleDto;
import com.mg.nmlonline.api.dto.ScenarioSummaryDto;
import com.mg.nmlonline.api.dto.TurnFinalizeResultDto;
import com.mg.nmlonline.api.dto.TurnResolutionStateDto;
import com.mg.nmlonline.domain.model.battle.BattleLogEntry;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.building.Building;
import com.mg.nmlonline.domain.model.building.BuildingType;
import com.mg.nmlonline.domain.model.building.WeaponCache;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentCategory;
import com.mg.nmlonline.domain.model.movement.MovementOrder;
import com.mg.nmlonline.domain.model.movement.MovementStatus;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.EntityCategory;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.domain.model.vehicle.VehicleType;
import com.mg.nmlonline.infrastructure.repository.BattleReportRepository;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.GameCharacterRepository;
import com.mg.nmlonline.infrastructure.repository.MovementOrderRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Scénarios de fin de tour sur PostgreSQL réel, orchestrateur + seeder : conflit résolu manuellement,
 * vainqueur d'une bataille intermédiaire, anéantissement mutuel, cascades FK (Phase 2/3), impasse
 * mexicaine 3 camps, scénario Lurio→Cegorach 2 hops, catalogue des 10 arènes de combat et leurs
 * résolutions de bout en bout. Déterministe (pas d'évasion), un contexte Spring neuf par test.
 */
@EmbeddedPostgresTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DisplayName("Résolution de tour — scénarios de bout en bout")
class TurnResolutionScenariosTest {

    @Autowired
    private TurnResolutionScenarioSeeder seeder;

    @Autowired
    private TurnResolutionOrchestrator orchestrator;

    @Autowired
    private TurnService turnService;

    @Autowired
    private MovementOrderRepository movementOrderRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private GameCharacterRepository gameCharacterRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private BattleReportRepository battleReportRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private PlatformTransactionManager txManager;

    @AfterEach
    void releaseLockAndCleanPendingOrders() {
        // Libère le verrou si une assertion a échoué avant finalizeTurn, et purge un ordre PENDING
        // qu'un test non transactionnel interrompu aurait laissé.
        orchestrator.abort();
        movementOrderRepository.deleteAll(
                movementOrderRepository.findPendingByTurn(turnService.getCurrentTurn()));
    }

    @Test
    @DisplayName("le vainqueur d'une bataille intermédiaire garde le secteur même si son unité repart au hop suivant")
    @Transactional
    void battleWinnerKeepsIntermediateSectorAfterMovingOn() {
        Board board = boardRepository.findAll().stream().findFirst().orElseThrow();
        List<Player> players = playerRepository.findAll();
        Player attacker = players.get(0);
        Player defender = players.stream()
                .filter(p -> !p.getId().equals(attacker.getId()))
                .findFirst().orElseThrow();

        Sector departure = board.getSector(7);
        Sector contested = board.getSector(9);
        Sector destination = board.getSector(14);
        assertTrue(contested.isNeutral() && contested.getArmySize() == 0,
                "Prérequis : le secteur 9 est neutre et vide");

        Unit leger = new Unit(8.0, UnitClass.LEGER);
        leger.setPlayerId(attacker.getId());
        departure.addUnit(leger);
        Unit larbin = new Unit(0.0, UnitClass.TIREUR);
        larbin.setPlayerId(defender.getId());
        contested.addUnit(larbin);
        em.flush();

        int turn = turnService.getCurrentTurn();
        movementOrderRepository.deleteAll(movementOrderRepository.findPendingByTurn(turn));
        em.flush();

        MovementOrder order = MovementOrder.createFootOrder(attacker.getId(), turn, List.of(leger.getId()),
                List.of(departure.getNumber(), contested.getNumber(), destination.getNumber()));
        movementOrderRepository.save(order);
        em.flush();

        orchestrator.startSession();
        TurnResolutionStateDto state = orchestrator.advanceHop();
        assertEquals(1, state.getPendingConflicts().size(), "Conflit attendu au secteur intermédiaire");
        PendingConflictDto conflict = state.getPendingConflicts().getFirst();
        assertEquals(contested.getNumber(), conflict.getSectorNumber());
        ResolvedBattleDto report = orchestrator.resolveBattle(conflict.getConflictId());
        assertTrue(report.isSuccess());
        assertEquals(attacker.getId(), report.getWinnerId(), "Le LEGER anéantit le LARBIN");
        assertFalse(battleReportRepository.findByParticipantId(defender.getId(), Pageable.unpaged()).isEmpty(),
                "Un rapport de combat est persisté pour les joueurs impliqués");

        state = orchestrator.advanceHop();
        assertEquals(2, state.getCurrentStep(), "Le LEGER repart vers le secteur 14");
        TurnFinalizeResultDto fin = orchestrator.finalizeTurn();

        em.flush();
        em.clear();
        Sector contestedRefreshed = boardRepository.findAll().stream()
                .findFirst().orElseThrow().getSector(contested.getNumber());
        assertEquals(attacker.getId(), contestedRefreshed.getOwnerId(),
                "Le vainqueur de la bataille garde le contrôle du secteur quitté");
        assertTrue(fin.getCapturedSectors().stream()
                        .anyMatch(c -> c.getSectorNumber() == contested.getNumber()),
                "La capture du secteur intermédiaire est rapportée");
    }

    @Test
    @DisplayName("anéantissement mutuel : le secteur contesté reste neutre")
    @Transactional
    void mutualAnnihilationLeavesSectorNeutral() {
        Board board = boardRepository.findAll().stream().findFirst().orElseThrow();
        List<Player> players = playerRepository.findAll();
        Player attacker = players.get(0);
        Player defender = players.stream()
                .filter(p -> !p.getId().equals(attacker.getId()))
                .findFirst().orElseThrow();

        Sector s1 = board.getAllSectors().stream()
                .filter(s -> s.isNeutral() && s.getArmySize() == 0)
                .findFirst().orElseThrow(() -> new AssertionError("Aucun secteur neutre vide pour s1"));
        Sector s2 = board.getAllSectors().stream()
                .filter(s -> s.isNeutral() && s.getArmySize() == 0 && s.getNumber() != s1.getNumber())
                .findFirst().orElseThrow(() -> new AssertionError("Aucun secteur neutre vide pour s2"));

        // 10 atk vs 10 def : les deux LARBINs se détruisent, le défenseur est déclaré vainqueur sans survivant.
        Unit attaquant = new Unit(0.0, UnitClass.TIREUR);
        attaquant.setPlayerId(attacker.getId());
        s1.addUnit(attaquant);
        Unit defenseur = new Unit(0.0, UnitClass.TIREUR);
        defenseur.setPlayerId(defender.getId());
        s2.addUnit(defenseur);
        em.flush();

        int turn = turnService.getCurrentTurn();
        movementOrderRepository.deleteAll(movementOrderRepository.findPendingByTurn(turn));
        em.flush();

        MovementOrder order = MovementOrder.createFootOrder(attacker.getId(), turn, List.of(attaquant.getId()),
                List.of(s1.getNumber(), s2.getNumber()));
        movementOrderRepository.save(order);
        em.flush();

        orchestrator.startSession();
        TurnResolutionStateDto state = orchestrator.advanceHop();
        ResolvedBattleDto report = orchestrator.resolveBattle(state.getPendingConflicts().getFirst().getConflictId());
        assertEquals(defender.getId(), report.getWinnerId(), "Les deux camps sont anéantis : le défenseur est vainqueur");
        assertEquals(1, report.getAttackerCasualties());
        assertEquals(1, report.getDefenderCasualties());

        TurnFinalizeResultDto fin = orchestrator.finalizeTurn();

        em.flush();
        em.clear();
        Sector s2Refreshed = boardRepository.findAll().stream()
                .findFirst().orElseThrow().getSector(s2.getNumber());
        assertNull(s2Refreshed.getOwnerId(), "Sans survivant, le vainqueur ne prend pas le secteur neutre");
        assertTrue(fin.getCapturedSectors().stream().noneMatch(c -> c.getSectorNumber() == s2.getNumber()));
    }

    @Test
    @DisplayName("resolveBattle détruit un défenseur équipé : la FK cascade efface ses rows unit_equipments (Phase 2)")
    void resolveBattle_perteUniteEquipee_effaceRowsUnitEquipmentsViaFkCascade() {
        // Pas de @Transactional sur ce test : setup commité pour que l'orchestrateur voie les données.
        Long ueId = new TransactionTemplate(txManager).execute(status -> {
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
            Long innerUeId = larbin.getUnitEquipments().get(0).getId();
            assertNotNull(innerUeId);

            int turn = turnService.getCurrentTurn();
            movementOrderRepository.deleteAll(movementOrderRepository.findPendingByTurn(turn));
            em.flush();

            MovementOrder order = MovementOrder.createFootOrder(
                    attacker.getId(), turn, List.of(brute.getId()),
                    List.of(s1.getNumber(), s2.getNumber()));
            movementOrderRepository.save(order);
            em.flush();
            return innerUeId;
        });

        orchestrator.startSession();
        TurnResolutionStateDto state = orchestrator.advanceHop();
        int conflictId = state.getPendingConflicts().get(0).getConflictId();
        ResolvedBattleDto report = orchestrator.resolveBattle(conflictId);

        assertTrue(report.isSuccess());
        assertEquals(1, report.getDefenderCasualties(),
                "Le défenseur équipé est détruit par le BRUTE 100/100");

        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            long ueCount = em.createQuery(
                            "select count(ue) from UnitEquipment ue where ue.id = :id", Long.class)
                    .setParameter("id", ueId)
                    .getSingleResult();
            assertEquals(0, ueCount,
                    "la row unit_equipments doit être effacée par la cascade REMOVE quand la Unit est DELETEd");
        });
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
        // Le VOYOU équipé (Tesla Carbine : pdf 60) entame le BRUTE n°2 en phase PDF (def 100 → 40),
        // puis meurt en phase bâtiments secondaires (Cache 100 + Banque 50 vs 20 def + 10 armure).
        assertEquals(1, report.getDefenderInjured(),
                "Le BRUTE n°2 (def 40 < 100 après le pdf de l'attaquant) termine blessé");
        assertEquals(cegorachId, report.getWinnerId(),
                "Attaquant anéanti ⇒ le défenseur garde le secteur (règle §2 du plan combat)");
        assertEquals(0, report.getCapturedBuildings(), "Pas de capture : le défenseur a gagné");
        assertFalse(report.isDefenderCharacterLost(),
                "Le personnage de cegorach combat au secteur 32 mais survit : l'attaquant est anéanti avant lui");

        // Pas de @Transactional sur ce test : un @Transactional de test masquerait la DataIntegrityViolationException au commit.
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

    @Test
    @Transactional
    @DisplayName("seedScenario crée un ordre PENDING route [41, 13, 32] et l'idempotence évite les doublons")
    void seedScenario_creeUnOrdreValideUnSeederNeDoublePas() {
        int turn = turnService.getCurrentTurn();

        ScenarioSummaryDto result = seeder.seedScenario();

        assertEquals(turn, result.getTurn());
        assertEquals(List.of(41, 13, 32), result.getRoute());
        assertNotNull(result.getOrderId());
        assertNotNull(result.getAttackerUnit());
        assertNotNull(result.getDefender());
        assertEquals("cegorach", result.getDefender().getName());

        var createdOrders = movementOrderRepository.findByPlayerIdAndTurnAndStatus(
                result.getAttacker().getId(), turn, MovementStatus.PENDING);
        assertEquals(1, createdOrders.size(),
                "Exactement un ordre PENDING pour lurio après seed");
        assertEquals(List.of(41, 13, 32), createdOrders.get(0).getRoute());

        ScenarioSummaryDto secondCall = seeder.seedScenario();
        var stillPending = movementOrderRepository.findByPlayerIdAndTurnAndStatus(
                secondCall.getAttacker().getId(), turn, MovementStatus.PENDING);
        assertEquals(1, stillPending.size(),
                "Re-seed ne crée pas de doublon d'ordre PENDING pour lurio");
        assertEquals(0, secondCall.getDefendersAdded(),
                "Au second appel, aucun défenseur n'est ré-ajouté (idempotence)");
    }

    @Test
    @Transactional
    @DisplayName("Re-seed sans attaquant éligible en 41 : la route repart de 13 au lieu d'échouer sur 41")
    void reseedWithAttackerInIntermediateSectorUsesRemainingRoute() {
        ScenarioSummaryDto first = seeder.seedScenario();
        Board board = boardRepository.findAll().stream().findFirst().orElseThrow();
        List<Unit> movers = board.getSector(41).getUnits().stream()
                .filter(u -> first.getAttacker().getId().equals(u.getPlayerId()))
                .filter(u -> u.getMaxMovementHops() >= 2)
                .toList();
        movers.forEach(unit -> {
            board.getSector(41).removeUnit(unit);
            board.getSector(13).addUnit(unit);
        });

        ScenarioSummaryDto reseed = seeder.seedScenario();

        assertEquals(List.of(13, 32), reseed.getRoute());
        assertEquals(13, reseed.getAttackerUnit().getFromSector());
        assertEquals(List.of(13, 32), movementOrderRepository
                .findByPlayerIdAndTurnAndStatus(reseed.getAttacker().getId(), reseed.getTurn(), MovementStatus.PENDING)
                .getFirst().getRoute());
    }

    @Test
    @Transactional
    @DisplayName("Catalogue : scénarios de combat avec arène neutre dédiée et points à observer")
    void listCombatScenarios_exposeLeCatalogue() {
        var scenarios = seeder.listCombatScenarios();

        assertEquals(10, scenarios.size());
        assertTrue(scenarios.stream().anyMatch(s -> "CREW_DISEMBARK".equals(s.getCode())
                && s.getArenaSector() == 24 && s.getStagingSector() == 19));
        assertTrue(scenarios.stream().anyMatch(s -> "BLDG_CAPTURE".equals(s.getCode())
                && s.getArenaSector() == 5 && s.getStagingSector() == 4));
        assertTrue(scenarios.stream().allMatch(s -> !s.getObservations().isEmpty()),
                "Chaque scénario documente ce qu'il faut vérifier dans le journal");
    }

    @Test
    @Transactional
    @DisplayName("seedCombatScenario : arène purgée et neutre, participants exacts, re-seed sans doublon")
    void seedCombatScenario_purgeEtRejoue() {
        ScenarioSummaryDto first = seeder.seedCombatScenario("CREW_DISEMBARK");

        assertEquals("CREW_DISEMBARK", first.getScenarioCode());
        assertEquals(List.of(19, 24), first.getRoute());

        var board = boardRepository.findAll().stream().findFirst().orElseThrow();
        var arena = board.getSector(24);
        var staging = board.getSector(19);
        assertNull(arena.getOwnerId(), "L'arène dédiée reste neutre");
        assertEquals(1, arena.getVehicles().size());
        assertEquals(VehicleType.VTT_LEGER, arena.getVehicles().getFirst().getVehicleType());
        assertTrue(arena.getVehicles().getFirst().hasPilot());
        assertEquals(1, arena.getUnits().size(), "Seul le pilote embarqué défend l'arène");
        assertEquals(2, staging.getUnits().size(), "Les deux attaquants attendent en 19");

        seeder.seedCombatScenario("CREW_DISEMBARK");

        assertEquals(1, movementOrderRepository.findByPlayerIdAndTurnAndStatus(
                        first.getAttacker().getId(), first.getTurn(), MovementStatus.PENDING).size(),
                "Re-seed ne crée pas de second ordre pour la même arène");
        assertEquals(1, arena.getVehicles().size(), "Le véhicule précédent est purgé avant le nouveau");
        assertEquals(1, arena.getUnits().size());
        assertEquals(2, staging.getUnits().size());
    }

    @Test
    @Transactional
    @DisplayName("Les 10 scénarios coexistent : arène, défenseur attendu et ordre PENDING pour chacun")
    void allCombatScenarios_coexistent() {
        var scenarios = seeder.listCombatScenarios();
        for (var scenario : scenarios) {
            seeder.seedCombatScenario(scenario.getCode());
        }

        var board = boardRepository.findAll().stream().findFirst().orElseThrow();
        assertEquals(VehicleType.VTT_LEGER, board.getSector(21).getVehicles().getFirst().getVehicleType());
        assertFalse(board.getSector(21).getVehicles().getFirst().hasPilot(), "VTT passif du scénario 50/50");
        assertEquals(VehicleType.TANK, board.getSector(42).getVehicles().getFirst().getVehicleType());
        assertEquals(VehicleType.VTT_BLINDE, board.getSector(35).getVehicles().getFirst().getVehicleType());
        assertEquals(1, board.getSector(35).getVehicles().getFirst().getPassengerCount());
        assertEquals(VehicleType.VTT_LEGER, board.getSector(24).getVehicles().getFirst().getVehicleType());
        assertEquals(VehicleType.HELICOPTERE, board.getSector(18).getVehicles().getFirst().getVehicleType());
        assertEquals(VehicleType.HELICOPTERE, board.getSector(28).getVehicles().getFirst().getVehicleType());

        assertTrue(board.getSector(37).getUnits().stream().allMatch(unit -> unit.getPdf() > 0),
                "Les MALFRATs du scénario Tank sont équipés du Gauss Cannon");
        assertEquals(3, board.getSector(5).getBuildings().size(), "Banque + Cache + QG du scénario de capture");
        assertTrue(board.getSector(5).getUnits().isEmpty(), "Aucune unité ne défend l'arène de capture");
        assertEquals(3, board.getSector(23).getBuildings().size(), "3 bâtiments du scénario de destruction");
        assertEquals(4, board.getSector(29).getUnits().size(), "Les 4 BRUTEs attendent en 29");
        assertEquals(1, board.getSector(9).getUnits().size(), "Seul le LARBIN défend l'arène du duel de personnage");
        assertEquals(2, board.getSector(7).getUnits().size(), "Les 2 MALFRATs attendent en 7");
        assertEquals(1, board.getSector(12).getCharacters().size(), "Personnage du scénario de régénération");
        assertEquals(1, board.getSector(31).getUnits().size(), "Le MALFRAT attend en 31");

        assertEquals(10, movementOrderRepository.findPendingByTurn(turnService.getCurrentTurn()).size(),
                "Chaque scénario dépose son propre ordre PENDING");
    }

    @Test
    @DisplayName("Scénario équipage : 1 hop puis résolution, le journal trace l'équipage débarqué")
    void crewDisembarkScenario_resolvesWithDisembarkLog() {
        seeder.seedCombatScenario("CREW_DISEMBARK");

        orchestrator.startSession();
        TurnResolutionStateDto state = orchestrator.advanceHop();

        assertEquals(1, state.getPendingConflicts().size(), "Un conflit attendu après le hop");
        PendingConflictDto conflict = state.getPendingConflicts().getFirst();
        assertEquals(24, conflict.getSectorNumber(), "Le conflit se résout dans l'arène du scénario");

        ResolvedBattleDto report = orchestrator.resolveBattle(conflict.getConflictId());

        assertTrue(report.isSuccess());
        assertTrue(report.getBattleLog().stream()
                        .anyMatch(entry -> entry.getMessage().contains("équipage débarqué")),
                "Le véhicule détruit débarque son pilote dans le journal");
        assertTrue(report.getDefenderCasualties() >= 1, "Le VTT léger est détruit");
    }

    @Test
    @DisplayName("Scénario capture : Banque/Cache/QG capturés, cache vidée vers lurio, secteur attribué")
    void buildingCaptureScenario_resolvesWithCapturesAndSectorTransfer() {
        ScenarioSummaryDto seed = seeder.seedCombatScenario("BLDG_CAPTURE");
        Long lurioId = seed.getAttacker().getId();
        Long arenaOwnerId = new TransactionTemplate(txManager).execute(status -> {
            Board board = boardRepository.findAll().stream().findFirst().orElseThrow();
            return board.getSector(5).getBuildings().getFirst().getPlayerId();
        });
        double arenaOwnerMoneyBefore = new TransactionTemplate(txManager).execute(status ->
                playerRepository.findById(arenaOwnerId).orElseThrow().getStats().getMoney());

        orchestrator.startSession();
        PendingConflictDto conflict = orchestrator.advanceHop().getPendingConflicts().getFirst();
        assertEquals(5, conflict.getSectorNumber());

        ResolvedBattleDto report = orchestrator.resolveBattle(conflict.getConflictId());

        assertTrue(report.isSuccess());
        assertEquals(3, report.getCapturedBuildings(), "Banque, Cache et QG capturés");
        assertTrue(report.isDefenderHeadquartersCaptured());
        assertTrue(report.getBattleLog().stream().anyMatch(entry ->
                        entry.getMessage().contains("3 bâtiment(s) capturé(s) — quartier général capturé")),
                "Le bilan trace les trois captures");

        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            Board board = boardRepository.findAll().stream().findFirst().orElseThrow();
            assertEquals(lurioId, board.getSector(5).getOwnerId(), "L'arène bascule au vainqueur");
            List<Building> buildings = board.getSector(5).getBuildings();
            assertEquals(3, buildings.size());
            assertTrue(buildings.stream().allMatch(Building::isCaptured));
            WeaponCache cache = (WeaponCache) buildings.stream()
                    .filter(WeaponCache.class::isInstance)
                    .findFirst()
                    .orElseThrow();
            assertTrue(cache.getStoredEquipments().isEmpty(), "Le contenu de la cache rejoint le vainqueur");
            Player arenaOwner = playerRepository.findById(arenaOwnerId).orElseThrow();
            assertEquals(arenaOwnerMoneyBefore * 0.75, arenaOwnerMoneyBefore - arenaOwner.getStats().getMoney(),
                    0.001, "75 % de la fortune exposée est transférée au vainqueur");
        });
    }

    @Test
    @DisplayName("Scénario destruction : Banque et Cache rasées non capturées, QG capturé même détruit")
    void buildingDestroyScenario_onlyHeadquartersCaptured() {
        seeder.seedCombatScenario("BLDG_DESTROY");

        orchestrator.startSession();
        PendingConflictDto conflict = orchestrator.advanceHop().getPendingConflicts().getFirst();
        ResolvedBattleDto report = orchestrator.resolveBattle(conflict.getConflictId());

        assertTrue(report.isSuccess());
        assertEquals(1, report.getCapturedBuildings(), "Seul le QG est capturé");
        assertTrue(report.isDefenderHeadquartersCaptured());
        assertTrue(report.getBattleLog().stream()
                        .anyMatch(entry -> entry.getMessage().contains("détruit arene-ruines · Banque")),
                "La Banque tombe en premier (ordre d'exposition)");
        assertTrue(report.getBattleLog().stream()
                        .anyMatch(entry -> entry.getMessage().contains("détruit arene-ruines · Cache d'armes")));
        assertTrue(report.getBattleLog().stream()
                        .anyMatch(entry -> entry.getMessage().contains("détruit arene-ruines · Quartier Général")));
    }

    @Test
    @DisplayName("Scénario personnage : mort du personnage, perte signalée et +1 Exp de bonus")
    void characterKillScenario_losesCharacterAndGrantsExperience() {
        seeder.seedCombatScenario("CHAR_KILL");

        orchestrator.startSession();
        PendingConflictDto conflict = orchestrator.advanceHop().getPendingConflicts().getFirst();
        ResolvedBattleDto report = orchestrator.resolveBattle(conflict.getConflictId());

        assertTrue(report.isSuccess());
        assertTrue(report.isDefenderCharacterLost());
        assertTrue(report.getBattleLog().stream()
                        .anyMatch(entry -> entry.getMessage().contains("Personnage perdu : arene-personnages")),
                "Le bilan signale le personnage perdu");
        assertTrue(report.getBattleLog().stream()
                        .anyMatch(entry -> BattleLogEntry.GAIN.equals(entry.getOutcome())
                                && entry.getMessage().contains("Exp + 2 →")),
                "Participation (+1) + personnage éliminé (+1)");
    }

    @Test
    @DisplayName("Scénario régénération : aucun vainqueur, le personnage récupère 50 défense à la finalisation")
    void characterRegenScenario_regeneratesAfterFinalize() {
        seeder.seedCombatScenario("CHAR_REGEN");

        orchestrator.startSession();
        PendingConflictDto conflict = orchestrator.advanceHop().getPendingConflicts().getFirst();
        ResolvedBattleDto report = orchestrator.resolveBattle(conflict.getConflictId());

        assertTrue(report.isSuccess());
        assertNull(report.getWinnerId(), "Personnage survivant : pas de vainqueur");
        assertFalse(report.isDefenderCharacterLost());
        assertTrue(report.getBattleLog().stream()
                        .anyMatch(entry -> entry.getMessage().contains("Colosse d'arène touche")
                                && entry.getPhase().contains("Personnages")),
                "Le personnage riposte en phase Personnages");

        orchestrator.finalizeTurn();

        GameCharacter colosse = gameCharacterRepository.findByName("Colosse d'arène").orElseThrow();
        assertEquals(100.0, colosse.getDefense(), "100 → 60 après 40 dégâts, +50 plafonnés à la base 100");
    }
}
