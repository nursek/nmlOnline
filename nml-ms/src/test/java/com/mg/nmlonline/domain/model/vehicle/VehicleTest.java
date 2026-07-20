package com.mg.nmlonline.domain.model.vehicle;

import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de régression sur les véhicules :
 * table d'équilibrage des 6 types, stats recalculées, règles de pilote,
 * embarquement et mobilité.
 */
@DisplayName("Vehicle")
class VehicleTest {

    private Unit pilotUnit() {
        return new Unit(0, UnitClass.PILOTE_DESTRUCTEUR);
    }

    @Nested
    @DisplayName("Table d'équilibrage des types")
    class VehicleTypeBalanceTests {

        @Test
        @DisplayName("Coûts : 1300 / 4000 / 6500 / 7500 / 9000 / 15000")
        void shouldPinCosts() {
            assertEquals(1300, VehicleType.TOURELLE.getCost());
            assertEquals(4000, VehicleType.VTT_LEGER.getCost());
            assertEquals(6500, VehicleType.VTT_BLINDE.getCost());
            assertEquals(7500, VehicleType.TANK.getCost());
            assertEquals(9000, VehicleType.HELICOPTERE.getCost());
            assertEquals(15000, VehicleType.AVION_TRANSPORT.getCost());
        }

        @Test
        @DisplayName("Stats offensives/défensives par type")
        void shouldPinCombatStats() {
            assertEquals(25.0, VehicleType.TOURELLE.getBasePdf());
            assertEquals(40.0, VehicleType.TOURELLE.getBaseDefense());
            assertEquals(0.0, VehicleType.VTT_LEGER.getBasePdf());
            assertEquals(50.0, VehicleType.VTT_LEGER.getBaseDefense());
            assertEquals(100.0, VehicleType.VTT_BLINDE.getBasePdf());
            assertEquals(150.0, VehicleType.VTT_BLINDE.getBaseDefense());
            assertEquals(125.0, VehicleType.TANK.getBasePdf());
            assertEquals(250.0, VehicleType.TANK.getBaseDefense());
            assertEquals(250.0, VehicleType.HELICOPTERE.getBasePdf());
            assertEquals(125.0, VehicleType.HELICOPTERE.getBaseDefense());
            assertEquals(0.0, VehicleType.AVION_TRANSPORT.getBasePdf());
            assertEquals(1000.0, VehicleType.AVION_TRANSPORT.getBaseDefense());
        }

        @Test
        @DisplayName("Vitesses et capacités par type")
        void shouldPinSpeedAndCapacity() {
            assertEquals(2, VehicleType.TOURELLE.getSpeed());
            assertEquals(1, VehicleType.TOURELLE.getCapacity());
            assertEquals(2, VehicleType.VTT_LEGER.getSpeed());
            assertEquals(10, VehicleType.VTT_LEGER.getCapacity());
            assertEquals(1, VehicleType.TANK.getSpeed());
            assertEquals(0, VehicleType.TANK.getCapacity());
            assertEquals(2, VehicleType.HELICOPTERE.getSpeed());
            assertEquals(5, VehicleType.HELICOPTERE.getCapacity());
            assertEquals(4, VehicleType.AVION_TRANSPORT.getSpeed());
            assertEquals(50, VehicleType.AVION_TRANSPORT.getCapacity());
        }

        @Test
        @DisplayName("Flags spéciaux : résistance, feu en transit, aérien")
        void shouldPinSpecialFlags() {
            assertEquals(50, VehicleType.TANK.getResistance());
            assertEquals(0, VehicleType.VTT_LEGER.getResistance());

            assertTrue(VehicleType.VTT_BLINDE.isFiresInTransit());
            assertTrue(VehicleType.HELICOPTERE.isFiresInTransit());
            assertFalse(VehicleType.TANK.isFiresInTransit());

            assertTrue(VehicleType.HELICOPTERE.isAerial());
            assertTrue(VehicleType.AVION_TRANSPORT.isAerial());
            assertFalse(VehicleType.TANK.isAerial());
        }
    }

    @Nested
    @DisplayName("Stats recalculées")
    class StatsTests {

        @Test
        @DisplayName("Pdf/Def du type, atk/pdc/armor/evasion à 0")
        void shouldPinRecalculatedStats() {
            Vehicle vehicle = new Vehicle(VehicleType.VTT_BLINDE, 1L);

            assertEquals(100.0, vehicle.getPdf());
            assertEquals(150.0, vehicle.getDefense());
            assertEquals(0.0, vehicle.getAttack());
            assertEquals(0.0, vehicle.getPdc());
            assertEquals(0.0, vehicle.getArmor());
            assertEquals(0.0, vehicle.getEvasion());
        }

        @Test
        @DisplayName("Véhicule détruit : toutes stats à 0")
        void shouldZeroStatsWhenDestroyed() {
            Vehicle vehicle = new Vehicle(VehicleType.VTT_BLINDE, 1L);
            vehicle.setDestroyed(true);
            vehicle.recalculateBaseStats();

            assertEquals(0.0, vehicle.getPdf());
            assertEquals(0.0, vehicle.getDefense());
        }
    }

    @Nested
    @DisplayName("Pilote")
    class PilotTests {

        @Test
        @DisplayName("Unit sans PILOTE_DESTRUCTEUR ne peut pas piloter")
        void shouldRefuseUnitWithoutPilotClass() {
            Vehicle vehicle = new Vehicle(VehicleType.TANK, 1L);

            assertFalse(vehicle.assignPilot(new Unit(0, UnitClass.TIREUR)));
            assertFalse(vehicle.hasPilot());
        }

        @Test
        @DisplayName("Unit avec PILOTE_DESTRUCTEUR peut piloter")
        void shouldAcceptUnitWithPilotClass() {
            Vehicle vehicle = new Vehicle(VehicleType.TANK, 1L);

            assertTrue(vehicle.assignPilot(pilotUnit()));
            assertTrue(vehicle.hasPilot());
        }

        @Test
        @DisplayName("GameCharacter peut piloter sans restriction de classe")
        void shouldAcceptGameCharacterAsPilot() {
            Vehicle vehicle = new Vehicle(VehicleType.TANK, 1L);
            GameCharacter hero = new GameCharacter("Héros", 10, 5, 5, 10, 5, 5);

            assertTrue(vehicle.assignPilot(hero));
            assertTrue(vehicle.hasPilot());
        }

        @Test
        @DisplayName("Pilote null refusé, pilote détruit invalide")
        void shouldRejectNullOrDestroyedPilot() {
            Vehicle vehicle = new Vehicle(VehicleType.TANK, 1L);
            assertFalse(vehicle.assignPilot(null));

            Unit pilot = pilotUnit();
            vehicle.assignPilot(pilot);
            pilot.setDestroyed(true);

            assertFalse(vehicle.hasPilot());
        }

        @Test
        @DisplayName("removePilot détache le pilote")
        void shouldRemovePilot() {
            Vehicle vehicle = new Vehicle(VehicleType.TANK, 1L);
            Unit pilot = pilotUnit();
            vehicle.assignPilot(pilot);

            assertSame(pilot, vehicle.removePilot());
            assertFalse(vehicle.hasPilot());
        }
    }

    @Nested
    @DisplayName("Embarquement")
    class EmbarkTests {

        @Test
        @DisplayName("Capacité respectée (TOURELLE : 1 passager)")
        void shouldEnforceCapacity() {
            Vehicle vehicle = new Vehicle(VehicleType.TOURELLE, 1L);

            assertTrue(vehicle.embark(new Unit(0, UnitClass.TIREUR)));
            assertFalse(vehicle.embark(new Unit(0, UnitClass.TIREUR)));
            assertEquals(1, vehicle.getPassengerCount());
            assertEquals(0, vehicle.getRemainingCapacity());
        }

        @Test
        @DisplayName("Entité null ou détruite refusée")
        void shouldRefuseNullOrDestroyedPassenger() {
            Vehicle vehicle = new Vehicle(VehicleType.VTT_LEGER, 1L);
            assertFalse(vehicle.embark(null));

            Unit dead = new Unit(0, UnitClass.TIREUR);
            dead.setDestroyed(true);
            assertFalse(vehicle.embark(dead));
        }

        @Test
        @DisplayName("disembark et disembarkAll vident le véhicule")
        void shouldDisembark() {
            Vehicle vehicle = new Vehicle(VehicleType.VTT_LEGER, 1L);
            Unit pilot = pilotUnit();
            Unit first = new Unit(0, UnitClass.TIREUR);
            Unit second = new Unit(0, UnitClass.TIREUR);
            vehicle.assignPilot(pilot);
            vehicle.embark(first);
            vehicle.embark(second);

            assertEquals(3, vehicle.disembarkAll().size()); // pilote + 2 passagers
            assertEquals(0, vehicle.getPassengerCount());
            assertFalse(vehicle.hasPilot());
        }
    }

    @Nested
    @DisplayName("Mobilité et règles spéciales")
    class MobilityTests {

        @Test
        @DisplayName("cantMove : sans pilote vivant ou détruit")
        void shouldPinCantMoveRule() {
            Vehicle vehicle = new Vehicle(VehicleType.TANK, 1L);
            assertTrue(vehicle.cantMove()); // pas de pilote

            vehicle.assignPilot(pilotUnit());
            assertFalse(vehicle.cantMove());

            vehicle.setDestroyed(true);
            assertTrue(vehicle.cantMove());
        }

        @Test
        @DisplayName("Résistance : Tank 50%, autres 0%")
        void shouldPinResistancePercent() {
            assertEquals(0.5, new Vehicle(VehicleType.TANK, 1L).getResistancePercent());
            assertEquals(0.0, new Vehicle(VehicleType.VTT_LEGER, 1L).getResistancePercent());
        }

        @Test
        @DisplayName("Seul l'avion de transport ne participe pas au combat au sol")
        void shouldPinGroundCombatParticipation() {
            // comportement actuel piné : règle définie mais non enforcée dans Battle
            assertFalse(new Vehicle(VehicleType.AVION_TRANSPORT, 1L).participatesInGroundCombat());
            assertTrue(new Vehicle(VehicleType.TANK, 1L).participatesInGroundCombat());
            assertTrue(new Vehicle(VehicleType.TOURELLE, 1L).participatesInGroundCombat());
        }

        @Test
        @DisplayName("Délégations vitesse/capacité/aérien/feu en transit")
        void shouldDelegateToType() {
            Vehicle heli = new Vehicle(VehicleType.HELICOPTERE, 1L);

            assertEquals(2, heli.getSpeed());
            assertEquals(5, heli.getCapacity());
            assertTrue(heli.isAerial());
            assertTrue(heli.firesInTransit());
        }
    }
}
