package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import com.mg.nmlonline.domain.model.vehicle.VehicleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PlayerStatsService")
class PlayerStatsServiceTest {

    private PlayerStatsService statsService;
    private Player player;
    private Board board;

    @BeforeEach
    void setUp() {
        statsService = new PlayerStatsService();
        player = new Player("TestPlayer");
        player.setId(1L);
        board = new Board();
        board.addSector(new Sector(1, "Secteur 1"));
        board.addSector(new Sector(2, "Secteur 2"));
        board.addSector(new Sector(3, "Secteur 3"));
    }

    private void own(int sectorNumber) {
        board.assignOwner(sectorNumber, 1L, "#FF0000");
    }

    @Nested
    @DisplayName("Stats de combat")
    class CombatStatsTests {

        @Test
        @DisplayName("Agrégation des stats des unités de tous les secteurs possédés")
        void shouldAggregateUnitStatsAcrossOwnedSectors() {
            own(1);
            own(2);
            board.getSector(1).addUnit(new Unit(8, UnitClass.TIREUR));
            board.getSector(2).addUnit(new Unit(5, UnitClass.TIREUR));

            statsService.updateCombatStats(player, board);

            assertEquals(150.0, player.getStats().getTotalAtk());
            assertEquals(150.0, player.getStats().getTotalDef());
        }

        @Test
        @DisplayName("Les véhicules et bâtiments comptent dans les stats de combat")
        void shouldIncludeVehiclesInCombatStats() {
            own(1);
            board.getSector(1).addUnit(new Unit(8, UnitClass.TIREUR));
            board.getSector(1).getVehicles().add(new Vehicle(VehicleType.TOURELLE, 1L));

            statsService.updateCombatStats(player, board);

            assertEquals(100.0, player.getStats().getTotalAtk());
            assertEquals(25.0, player.getStats().getTotalPdf());
            assertEquals(140.0, player.getStats().getTotalDef());
        }

        @Test
        @DisplayName("Les secteurs des autres joueurs sont ignorés")
        void shouldIgnoreOtherPlayersSectors() {
            own(1);
            board.assignOwner(2, 2L, "#0000FF");
            board.getSector(1).addUnit(new Unit(8, UnitClass.TIREUR));
            board.getSector(2).addUnit(new Unit(8, UnitClass.TIREUR));

            statsService.updateCombatStats(player, board);

            assertEquals(100.0, player.getStats().getTotalAtk());
        }

        @Test
        @DisplayName("Joueur ou board null : aucune erreur")
        void shouldTolerateNullParameters() {
            assertDoesNotThrow(() -> statsService.updateCombatStats(null, board));
            assertDoesNotThrow(() -> statsService.updateCombatStats(player, null));
        }
    }

    @Nested
    @DisplayName("Puissance globale")
    class GlobalPowerTests {

        @Test
        @DisplayName("globalPower = (offensive + defensive) / 2")
        void shouldPinGlobalPowerFormula() {
            own(1);
            board.getSector(1).addUnit(new Unit(8, UnitClass.TIREUR));

            statsService.updateGlobalStats(player, board);

            assertEquals(100.0, player.getStats().getTotalOffensivePower());
            assertEquals(100.0, player.getStats().getTotalDefensivePower());
            assertEquals(100.0, player.getStats().getGlobalPower());
        }

        @Test
        @DisplayName("Offensive = atk + pdf + pdc, defensive = def + armor")
        void shouldPinOffensiveDefensiveSplit() {
            own(1);
            board.getSector(1).addUnit(new Unit(8, UnitClass.TIREUR));
            board.getSector(1).getVehicles().add(new Vehicle(VehicleType.TOURELLE, 1L));
            board.getSector(1).recalculateMilitaryPower();

            statsService.updateGlobalStats(player, board);

            assertEquals(125.0, player.getStats().getTotalOffensivePower());
            assertEquals(140.0, player.getStats().getTotalDefensivePower());
            assertEquals(132.5, player.getStats().getGlobalPower());
        }
    }

    @Nested
    @DisplayName("Revenus")
    class IncomeTests {

        @Test
        @DisplayName("totalIncome = somme des revenus des secteurs possédés (2000 par défaut)")
        void shouldSumSectorIncomes() {
            own(1);
            own(2);

            statsService.calculateTotalIncome(player, board);

            assertEquals(4000.0, player.getStats().getTotalIncome());
        }

        @Test
        @DisplayName("Revenu personnalisé d'un secteur pris en compte")
        void shouldUseCustomSectorIncome() {
            own(1);
            own(2);
            board.getSector(2).setIncome(500.0);

            statsService.calculateTotalIncome(player, board);

            assertEquals(2500.0, player.getStats().getTotalIncome());
        }

        @Test
        @DisplayName("Secteurs neutres et ennemis exclus du revenu")
        void shouldExcludeNonOwnedSectorsFromIncome() {
            own(1);
            board.assignOwner(2, 2L, "#0000FF");

            statsService.calculateTotalIncome(player, board);

            assertEquals(2000.0, player.getStats().getTotalIncome());
        }
    }

    @Nested
    @DisplayName("Recalcul complet")
    class RecalculateTests {

        @Test
        @DisplayName("recalculateStats chaîne combat + global + income + économie")
        void shouldChainAllCalculations() {
            own(1);
            own(2);
            board.getSector(1).addUnit(new Unit(8, UnitClass.TIREUR));
            player.getStats().setMoney(3000.0);

            statsService.recalculateStats(player, board);

            assertEquals(100.0, player.getStats().getTotalAtk());
            assertEquals(4000.0, player.getStats().getTotalIncome());
            assertEquals(7000.0, player.getStats().getTotalEconomyPower());
        }
    }

    @Nested
    @DisplayName("Secteurs avec entités combattantes")
    class SectorsWithCombatEntitiesTests {

        @Test
        @DisplayName("Inclut les secteurs possédés avec armée, exclut vides et neutres")
        void shouldFilterSectorsWithCombatEntities() {
            own(1);
            own(2);
            board.getSector(1).addUnit(new Unit(8, UnitClass.TIREUR));
            board.getSector(3).addUnit(new Unit(8, UnitClass.TIREUR));

            assertEquals(1, statsService.getSectorsWithCombatEntities(player, board).size());
            assertEquals(1, statsService.getSectorsWithCombatEntities(player, board).getFirst().getNumber());
        }
    }
}
