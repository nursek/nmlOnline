package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.EmbeddedPostgresTest;
import com.mg.nmlonline.api.dto.PendingConflictDto;
import com.mg.nmlonline.api.dto.ResolvedBattleDto;
import com.mg.nmlonline.api.dto.ScenarioSummaryDto;
import com.mg.nmlonline.api.dto.TurnResolutionStateDto;
import com.mg.nmlonline.domain.model.battle.BattleLogEntry;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.building.Building;
import com.mg.nmlonline.domain.model.building.WeaponCache;
import com.mg.nmlonline.domain.model.movement.MovementStatus;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.vehicle.VehicleType;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.GameCharacterRepository;
import com.mg.nmlonline.infrastructure.repository.MovementOrderRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@EmbeddedPostgresTest
@DisplayName("TurnResolutionScenarioSeeder — préparation du scénario de test")
class TurnResolutionScenarioSeederTest {

    @Autowired
    private TurnResolutionScenarioSeeder seeder;

    @Autowired
    private TurnService turnService;

    @Autowired
    private MovementOrderRepository movementOrderRepository;

    @Autowired
    private TurnResolutionOrchestrator orchestrator;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private GameCharacterRepository characterRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private PlatformTransactionManager txManager;

    @AfterEach
    void releaseLockAndCleanPendingOrders() {
        orchestrator.abort();
        // Un test non transactionnel interrompu peut laisser un ordre PENDING qui polluerait le suivant.
        movementOrderRepository.deleteAll(
                movementOrderRepository.findPendingByTurn(turnService.getCurrentTurn()));
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

        GameCharacter colosse = characterRepository.findByName("Colosse d'arène").orElseThrow();
        assertEquals(100.0, colosse.getDefense(), "100 → 60 après 40 dégâts, +50 plafonnés à la base 100");
    }
}
