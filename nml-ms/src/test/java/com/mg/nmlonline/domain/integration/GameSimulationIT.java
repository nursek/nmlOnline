package com.mg.nmlonline.domain.integration;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.domain.service.CombatService;
import com.mg.nmlonline.domain.service.PlayerStatsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration du scénario de simulation de jeu (sans Spring) :
 * bataille complète via CombatService et calculs de statistiques.
 *
 * Les unités n'ont ni équipement ni évasion : les combats sont
 * entièrement déterministes malgré le RNG non seedé de Battle.
 */
@DisplayName("Game Simulation Integration Tests")
class GameSimulationIT {

    private Player player1;
    private Player player2;
    private Player player3;
    private Board board;
    private PlayerStatsService playerStatsService;
    private CombatService combatService;

    @BeforeEach
    void setUp() {
        playerStatsService = new PlayerStatsService();
        combatService = new CombatService();
        // CombatService utilise l'injection par champ @Autowired : on la simule ici
        ReflectionTestUtils.setField(combatService, "playerStatsService", playerStatsService);

        player1 = new Player("Général Suprême");
        player1.setId(1L);
        player1.getStats().setMoney(20000.0);

        player2 = new Player("Baron de Guerre");
        player2.setId(2L);
        player2.getStats().setMoney(15000.0);

        player3 = new Player("Seigneur des Ombres");
        player3.setId(3L);
        player3.getStats().setMoney(18000.0);

        board = new Board();
    }

    // ponytail: board/sector/neighbor coverage lives in BoardTest; this IT keeps battle + stats

    @Nested
    @DisplayName("Simulation de bataille complète")
    class BattleSimulationTests {

        @BeforeEach
        void setUpBattleScenario() {
            setupCompleteBoard();
            setupGridNeighbors(4);
            addArmiesToSectors();
        }

        @Test
        @DisplayName("Bataille échoue si le défenseur n'a pas d'armée")
        void shouldFailBattleIfDefenderHasNoArmy() {
            Player noArmyPlayer = new Player("Sans Armée");
            noArmyPlayer.setId(99L);

            CombatService.BattleResult result = combatService.simulateBattle(player1, noArmyPlayer, board);

            assertFalse(result.success());
            assertNotNull(result.message());
        }

        @Test
        @DisplayName("Bataille échoue si l'attaquant n'a pas d'armée")
        void shouldFailBattleIfAttackerHasNoArmy() {
            Player noArmyPlayer = new Player("Sans Armée");
            noArmyPlayer.setId(99L);

            CombatService.BattleResult result = combatService.simulateBattle(noArmyPlayer, player1, board);

            assertFalse(result.success());
        }

        @Test
        @DisplayName("Bataille échoue sur paramètres null")
        void shouldFailBattleOnNullParameters() {
            assertFalse(combatService.simulateBattle(null, player1, board).success());
            assertFalse(combatService.simulateBattle(player1, null, board).success());
            assertFalse(combatService.simulateBattle(player1, player2, null).success());
        }

        @Test
        @DisplayName("Bataille player2 vs player3 : issue exacte déterministe")
        void shouldResolveDeterministicBattleOutcome() {
            // Attaquant (s3) : BRUTE 100/100 + MALFRAT 50/50 → 150 atk
            // Défenseur (s9) : 2 BRUTEs 100/100 → 200 atk
            // Sans évasion, le déroulé est exact :
            //   - défenseurs : 150 pts → 1 BRUTE détruit (coût 100), 1 BRUTE à 50 def
            //   - attaquants : 200 pts → MALFRAT détruit (coût 50), BRUTE détruit (coût 100)
            CombatService.BattleResult result = combatService.simulateBattle(player2, player3, board);

            assertTrue(result.success());
            assertNotNull(result.message());

            // comportement actuel piné : winner jamais assigné par Battle — à revoir
            assertNull(result.winner());

            // Armée attaquante anéantie
            assertEquals(0, board.getSector(3).getArmySize());

            // Un défenseur survit, blessé (def 50 < base 100) → stats ÷ 2
            Sector defenderSector = board.getSector(9);
            assertEquals(1, defenderSector.getArmySize());
            Unit survivor = defenderSector.getUnits().getFirst();
            assertTrue(survivor.isInjured());
            assertEquals(50.0, survivor.getAttack());
            assertEquals(50.0, survivor.getDefense());
        }
    }

    @Nested
    @DisplayName("Calcul des statistiques")
    class StatsCalculationTests {

        @BeforeEach
        void setUpStatsScenario() {
            setupCompleteBoard();
            addArmiesToSectors();
        }

        @Test
        @DisplayName("Revenu total = 4 secteurs × 2000 = 8000")
        void shouldRecalculateIncomeExactly() {
            playerStatsService.recalculateStats(player1, board);

            assertEquals(8000.0, player1.getStats().getTotalIncome());
        }

        @Test
        @DisplayName("Puissance de combat = somme exacte des stats des unités")
        void shouldRecalculateCombatPowerExactly() {
            playerStatsService.recalculateStats(player1, board);

            // 3 BRUTEs (100 atk / 100 def chacun) dans les secteurs 1 et 2
            assertEquals(300.0, player1.getStats().getTotalAtk());
            assertEquals(300.0, player1.getStats().getTotalDef());
        }

        @Test
        @DisplayName("Nombre de secteurs par joueur : 4, 4, 4 et 4 neutres")
        void shouldCountSectorsCorrectly() {
            assertEquals(4, board.getSectorsByOwner(1L).size());
            assertEquals(4, board.getSectorsByOwner(2L).size());
            assertEquals(4, board.getSectorsByOwner(3L).size());
            assertEquals(4, board.getNeutralSectors().size());
        }

        @Test
        @DisplayName("Secteurs avec armée de player1 : exactement 1 et 2")
        void shouldGetSectorsWithArmy() {
            List<Sector> sectorsWithArmy = playerStatsService.getSectorsWithCombatEntities(player1, board);

            assertEquals(2, sectorsWithArmy.size());
            assertEquals(1, sectorsWithArmy.get(0).getNumber());
            assertEquals(2, sectorsWithArmy.get(1).getNumber());
        }
    }

    // === Méthodes utilitaires pour setup ===

    private void setupCompleteBoard() {
        for (int i = 1; i <= 16; i++) {
            board.addSector(new Sector(i, "Secteur " + i));
        }
        for (int s : new int[]{1, 2, 4, 5}) {
            board.assignOwner(s, 1L, "#FF0000");
        }
        for (int s : new int[]{3, 6, 7, 8}) {
            board.assignOwner(s, 2L, "#0000FF");
        }
        for (int s : new int[]{9, 10, 11, 12}) {
            board.assignOwner(s, 3L, "#00FF00");
        }
        // Secteurs 13 à 16 neutres
    }

    private void setupGridNeighbors(int gridSize) {
        for (int i = 1; i <= gridSize * gridSize; i++) {
            Sector sector = board.getSector(i);
            if (sector == null) continue;

            int row = (i - 1) / gridSize;
            int col = (i - 1) % gridSize;

            if (col < gridSize - 1) sector.addNeighbor(i + 1);
            if (row < gridSize - 1) sector.addNeighbor(i + gridSize);
            if (col > 0) sector.addNeighbor(i - 1);
            if (row > 0) sector.addNeighbor(i - gridSize);
        }
    }

    private void addArmiesToSectors() {
        Sector s1 = board.getSector(1);
        s1.addUnit(new Unit(9.0, UnitClass.TIREUR));
        s1.addUnit(new Unit(9.0, UnitClass.MASTODONTE));

        Sector s2 = board.getSector(2);
        s2.addUnit(new Unit(8.5, UnitClass.LEGER));

        Sector s3 = board.getSector(3);
        s3.addUnit(new Unit(8.0, UnitClass.TIREUR));
        s3.addUnit(new Unit(7.0, UnitClass.MASTODONTE));

        Sector s6 = board.getSector(6);
        s6.addUnit(new Unit(6.0, UnitClass.LEGER));

        Sector s9 = board.getSector(9);
        s9.addUnit(new Unit(8.0, UnitClass.TIREUR));
        s9.addUnit(new Unit(8.0, UnitClass.PILOTE_DESTRUCTEUR));

        Sector s10 = board.getSector(10);
        s10.addUnit(new Unit(5.0, UnitClass.SNIPER));
    }
}
