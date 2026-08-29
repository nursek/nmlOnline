package com.mg.nmlonline.domain.model.player;

import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentCategory;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import com.mg.nmlonline.domain.model.vehicle.VehicleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Player — Économie")
class PlayerEconomyTest {

    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player("TestPlayer");
        player.setId(1L);
        player.getStats().setMoney(1000.0);
    }

    private Equipment equipment(String name, int cost) {
        return new Equipment(name, cost, 0, 0, 0, 0,
                Set.of(UnitClass.TIREUR), EquipmentCategory.FIREARM);
    }

    @Nested
    @DisplayName("Achat d'équipement (modèle)")
    class BuyEquipmentTests {

        @Test
        @DisplayName("Achat réussi débite l'argent et crée le stack")
        void shouldDebitMoneyAndCreateStackOnSuccess() {
            boolean result = player.buyEquipment(equipment("Pistolet", 200), 2);

            assertTrue(result);
            assertEquals(600.0, player.getStats().getMoney());
            assertEquals(1, player.getEquipments().size());
            assertEquals(2, player.getEquipments().getFirst().getQuantity());
            assertEquals(2, player.getEquipments().getFirst().getAvailable());
        }

        @Test
        @DisplayName("Achat réussi met à jour la valeur d'équipement et la puissance économique")
        void shouldUpdateEquipmentValueAndEconomyPower() {
            player.buyEquipment(equipment("Pistolet", 200), 2);

            assertEquals(400.0, player.getStats().getTotalEquipmentValue());
            assertEquals(1000.0, player.getStats().getTotalEconomyPower());
        }

        @Test
        @DisplayName("Achat refusé si fonds insuffisants, sans mutation")
        void shouldRefusePurchaseWhenInsufficientFunds() {
            boolean result = player.buyEquipment(equipment("Fusil", 2000), 1);

            assertFalse(result);
            assertEquals(1000.0, player.getStats().getMoney());
            assertTrue(player.getEquipments().isEmpty());
            assertEquals(0.0, player.getStats().getTotalEquipmentValue());
        }

        @Test
        @DisplayName("Achat refusé à coût exact égal au solde est accepté")
        void shouldAcceptPurchaseAtExactBalance() {
            boolean result = player.buyEquipment(equipment("Fusil", 500), 2);

            assertTrue(result);
            assertEquals(0.0, player.getStats().getMoney());
        }

        @Test
        @DisplayName("Achat refusé si équipement null ou quantité <= 0")
        void shouldRefuseNullEquipmentOrNonPositiveQuantity() {
            assertFalse(player.buyEquipment(null, 1));
            assertFalse(player.buyEquipment(equipment("Pistolet", 100), 0));
            assertFalse(player.buyEquipment(equipment("Pistolet", 100), -3));
            assertEquals(1000.0, player.getStats().getMoney());
            assertTrue(player.getEquipments().isEmpty());
        }

        @Test
        @DisplayName("Achats répétés du même équipement fusionnent en un seul stack")
        void shouldMergeSameEquipmentIntoOneStack() {
            player.buyEquipment(equipment("Pistolet", 100), 1);
            player.buyEquipment(equipment("Pistolet", 100), 2);

            assertEquals(1, player.getEquipments().size());
            assertEquals(3, player.getEquipments().getFirst().getQuantity());
            assertEquals(700.0, player.getStats().getMoney());
        }
    }

    @Nested
    @DisplayName("Achat de véhicule (modèle)")
    class BuyVehicleTests {

        @Test
        @DisplayName("Achat réussi débite le coût et incrémente la valeur des véhicules")
        void shouldDebitCostAndIncrementVehiclesValue() {
            player.getStats().setMoney(10000.0);

            Vehicle vehicle = player.buyVehicle(VehicleType.VTT_LEGER);

            assertNotNull(vehicle);
            assertEquals(VehicleType.VTT_LEGER, vehicle.getVehicleType());
            assertEquals(1L, vehicle.getPlayerId());
            assertEquals(6000.0, player.getStats().getMoney());
            assertEquals(4000.0, player.getStats().getTotalVehiclesValue());
            assertEquals(10000.0, player.getStats().getTotalEconomyPower());
        }

        @Test
        @DisplayName("Achat refusé si fonds insuffisants")
        void shouldReturnNullWhenInsufficientFunds() {
            Vehicle vehicle = player.buyVehicle(VehicleType.TANK);

            assertNull(vehicle);
            assertEquals(1000.0, player.getStats().getMoney());
            assertEquals(0.0, player.getStats().getTotalVehiclesValue());
        }

        @Test
        @DisplayName("Achat refusé si type null")
        void shouldReturnNullForNullType() {
            assertNull(player.buyVehicle(null));
            assertEquals(1000.0, player.getStats().getMoney());
        }
    }

    @Nested
    @DisplayName("Gestion de l'argent")
    class MoneyTests {

        @Test
        @DisplayName("incrementMoney ajoute et recalcule la puissance économique")
        void shouldIncrementMoney() {
            player.incrementMoney(500.0);

            assertEquals(1500.0, player.getStats().getMoney());
            assertEquals(1500.0, player.getStats().getTotalEconomyPower());
        }

        @Test
        @DisplayName("incrementMoney ignore les montants <= 0")
        void shouldIgnoreNonPositiveIncrements() {
            player.incrementMoney(0.0);
            player.incrementMoney(-200.0);

            assertEquals(1000.0, player.getStats().getMoney());
        }

        @Test
        @DisplayName("decrementMoney retire le montant")
        void shouldDecrementMoney() {
            player.decrementMoney(300.0);

            assertEquals(700.0, player.getStats().getMoney());
            assertEquals(700.0, player.getStats().getTotalEconomyPower());
        }

        @Test
        @DisplayName("decrementMoney refuse silencieusement si solde insuffisant")
        void shouldSilentlyRefuseDecrementAboveBalance() {
            player.decrementMoney(10000.0);

            assertEquals(1000.0, player.getStats().getMoney());
        }

        @Test
        @DisplayName("decrementMoney ignore les montants <= 0")
        void shouldIgnoreNonPositiveDecrements() {
            player.decrementMoney(0.0);
            player.decrementMoney(-50.0);

            assertEquals(1000.0, player.getStats().getMoney());
        }
    }

    @Nested
    @DisplayName("Formules de puissance économique")
    class EconomyPowerFormulaTests {

        @Test
        @DisplayName("economyPower = income + equipmentValue + money + vehiclesValue")
        void shouldPinEconomyPowerFormula() {
            player.getStats().setTotalIncome(2000.0);
            player.buyEquipment(equipment("Pistolet", 200), 2);
            player.getStats().setMoney(6600.0);
            player.buyVehicle(VehicleType.VTT_LEGER);
            player.getStats().setTotalIncome(2000.0);
            player.calculateTotalEconomyPower();

            assertEquals(9000.0, player.getStats().getTotalEconomyPower());
        }

        @Test
        @DisplayName("totalEquipmentValue = somme(coût × quantité), même si équipé")
        void shouldPinTotalEquipmentValueFormula() {
            player.getStats().setMoney(100000.0);
            player.buyEquipment(equipment("A", 500), 2);
            player.buyEquipment(equipment("B", 300), 3);

            assertEquals(1900.0, player.getStats().getTotalEquipmentValue());

            // la valeur compte la quantité totale, pas la dispo : équiper ne change pas la valeur
            player.decrementEquipmentAvailability("A");

            assertEquals(1900.0, player.getStats().getTotalEquipmentValue());
        }
    }
}
