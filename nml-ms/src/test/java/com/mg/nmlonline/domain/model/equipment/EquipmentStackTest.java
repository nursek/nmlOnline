package com.mg.nmlonline.domain.model.equipment;

import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EquipmentStack et gestion d'inventaire")
class EquipmentStackTest {

    private Equipment firearm(String name) {
        return new Equipment(name, 100, 25, 0, 0, 0,
                Set.of(UnitClass.TIREUR), EquipmentCategory.FIREARM);
    }

    private Equipment melee(String name) {
        return new Equipment(name, 100, 0, 25, 0, 0,
                Set.of(UnitClass.TIREUR), EquipmentCategory.MELEE);
    }

    @Nested
    @DisplayName("Compteurs du stack")
    class StackCounterTests {

        @Test
        @DisplayName("increment monte quantity ET available")
        void shouldIncrementBothCounters() {
            EquipmentStack stack = new EquipmentStack(firearm("Pistolet"));
            stack.increment();

            assertEquals(2, stack.getQuantity());
            assertEquals(2, stack.getAvailable());
        }

        @Test
        @DisplayName("decrement descend les deux, plancher à 0")
        void shouldDecrementWithZeroFloor() {
            EquipmentStack stack = new EquipmentStack(firearm("Pistolet"));
        stack.decrement();
        stack.decrement();

            assertEquals(0, stack.getQuantity());
            assertEquals(0, stack.getAvailable());
        }

        @Test
        @DisplayName("decrementAvailable ne touche pas quantity, plancher à 0")
        void shouldDecrementAvailableOnly() {
            EquipmentStack stack = new EquipmentStack(firearm("Pistolet"));
            stack.decrementAvailable();
            stack.decrementAvailable();

            assertEquals(1, stack.getQuantity());
            assertEquals(0, stack.getAvailable());
            assertFalse(stack.isAvailable());
        }

        @Test
        @DisplayName("incrementAvailable est plafonné à quantity")
        void shouldCapAvailableAtQuantity() {
            EquipmentStack stack = new EquipmentStack(firearm("Pistolet"));
        stack.decrementAvailable();
        stack.incrementAvailable();
        stack.incrementAvailable();

            assertEquals(1, stack.getAvailable());
            assertTrue(stack.isAvailable());
        }
    }

    @Nested
    @DisplayName("Inventaire du joueur")
    class PlayerInventoryTests {

        private Player player;

        @BeforeEach
        void setUp() {
            player = new Player("TestPlayer");
            player.setId(1L);
            player.getStats().setMoney(100000.0);
        }

        @Test
        @DisplayName("addEquipmentToStack fusionne par nom")
        void shouldMergeStacksByName() {
            player.addEquipmentToStack(firearm("Pistolet"), 1);
            player.addEquipmentToStack(firearm("Pistolet"), 2);

            assertEquals(1, player.getEquipments().size());
            assertEquals(3, player.getEquipments().getFirst().getQuantity());
        }

        @Test
        @DisplayName("Noms différents créent des stacks distincts")
        void shouldCreateDistinctStacks() {
            player.addEquipmentToStack(firearm("Pistolet"), 1);
            player.addEquipmentToStack(firearm("Fusil"), 1);

            assertEquals(2, player.getEquipments().size());
        }

        @Test
        @DisplayName("removeEquipmentFromStack décrémente puis supprime à 1")
        void shouldDecrementThenRemoveStack() {
            player.addEquipmentToStack(firearm("Pistolet"), 2);
            Equipment gun = player.getEquipmentByString("Pistolet");

            player.removeEquipmentFromStack(gun);
            assertEquals(1, player.getEquipments().getFirst().getQuantity());

            player.removeEquipmentFromStack(gun);
            assertTrue(player.getEquipments().isEmpty());
        }

        @Test
        @DisplayName("removeEquipmentFromStack ignore la disponibilité")
        void shouldPinRemovalIgnoringAvailability() {
            // available < quantity : la suppression ignore la dispo (exemplaires équipés perdus).
            player.addEquipmentToStack(firearm("Pistolet"), 1);
            Equipment gun = player.getEquipmentByString("Pistolet");
            player.decrementEquipmentAvailability("Pistolet");

            player.removeEquipmentFromStack(gun);

            assertTrue(player.getEquipments().isEmpty());
        }

        @Test
        @DisplayName("isEquipmentAvailable reflète le compteur available")
        void shouldTrackAvailability() {
            player.addEquipmentToStack(firearm("Pistolet"), 1);

            assertTrue(player.isEquipmentAvailable("Pistolet"));
            player.decrementEquipmentAvailability("Pistolet");
            assertFalse(player.isEquipmentAvailable("Pistolet"));
            assertFalse(player.isEquipmentAvailable("Inconnu"));
        }
    }

    @Nested
    @DisplayName("Remplacement d'équipement")
    class ReplaceEquipmentTests {

        private Player player;
        private Unit unit;

        @BeforeEach
        void setUp() {
            player = new Player("TestPlayer");
            player.setId(1L);
            player.getStats().setMoney(100000.0);
            unit = new Unit(5, UnitClass.TIREUR);
        }

        @Test
        @DisplayName("Swap réussi : ancien rendu à l'inventaire, nouveau équipé")
        void shouldSwapEquipment() {
            Equipment oldGun = firearm("VieuxPistolet");
            Equipment newGun = firearm("NouveauPistolet");
            player.addEquipmentToStack(oldGun, 1);
            player.addEquipmentToStack(newGun, 1);
            unit.addEquipment(oldGun);
            player.decrementEquipmentAvailability("VieuxPistolet");

            boolean result = player.replaceEquipment(unit, oldGun, newGun);

            assertTrue(result);
            assertTrue(unit.getEquipments().contains(newGun));
            assertFalse(unit.getEquipments().contains(oldGun));
            assertTrue(player.isEquipmentAvailable("VieuxPistolet"));
            assertFalse(player.isEquipmentAvailable("NouveauPistolet"));
        }

        @Test
        @DisplayName("Nouvel équipement indisponible → refus sans mutation")
        void shouldRefuseUnavailableNewEquipment() {
            Equipment oldGun = firearm("VieuxPistolet");
            player.addEquipmentToStack(oldGun, 1);
            unit.addEquipment(oldGun);
            player.decrementEquipmentAvailability("VieuxPistolet");
            Equipment notOwned = firearm("PasEnStock");

            boolean result = player.replaceEquipment(unit, oldGun, notOwned);

            assertFalse(result);
            assertTrue(unit.getEquipments().contains(oldGun));
        }

        @Test
        @DisplayName("Échec d'équipement → rollback de l'ancien")
        void shouldRollbackOldEquipmentOnFailure() {
            Equipment oldGun = firearm("VieuxPistolet");
            player.addEquipmentToStack(oldGun, 1);
            unit.addEquipment(oldGun);
            player.decrementEquipmentAvailability("VieuxPistolet");
            // Incompatible avec l'unité (classe SNIPER requise) mais présent en stock
            Equipment incompatible = new Equipment("FusilSniper", 100, 50, 0, 0, 0,
                    Set.of(UnitClass.SNIPER), EquipmentCategory.FIREARM);
            player.addEquipmentToStack(incompatible, 1);

            boolean result = player.replaceEquipment(unit, oldGun, incompatible);

            assertFalse(result);
            assertTrue(unit.getEquipments().contains(oldGun));
            assertFalse(player.isEquipmentAvailable("VieuxPistolet"));
            assertTrue(player.isEquipmentAvailable("FusilSniper"));
        }

        @Test
        @DisplayName("replaceEquipmentByCategory évince le PREMIER de la catégorie à la limite")
        void shouldPinFifoEvictionByCategory() {
            Equipment blade1 = melee("Couteau");
            Equipment blade2 = melee("Hyperphase Sword");
            Equipment blade3 = melee("Katana");
            player.addEquipmentToStack(blade1, 1);
            player.addEquipmentToStack(blade2, 1);
            player.addEquipmentToStack(blade3, 1);
            unit.addEquipment(blade1);
            unit.addEquipment(blade2);

            // à la limite, le premier équipé de la catégorie est évincé silencieusement (FIFO)
            boolean result = player.replaceEquipmentByCategory(unit, blade3);

            assertTrue(result);
            assertFalse(unit.getEquipments().contains(blade1));
            assertTrue(unit.getEquipments().contains(blade2));
            assertTrue(unit.getEquipments().contains(blade3));
            assertTrue(player.isEquipmentAvailable("Couteau"));
        }

        @Test
        @DisplayName("getCompatibleEquipments filtre disponibles et compatibles")
        void shouldFilterCompatibleEquipments() {
            player.addEquipmentToStack(firearm("Pistolet"), 1);
            player.addEquipmentToStack(melee("Couteau"), 1);
            player.decrementEquipmentAvailability("Couteau");

            assertEquals(1, player.getCompatibleEquipments(unit).size());
            assertEquals(0, player.getCompatibleEquipmentsByCategory(unit, EquipmentCategory.MELEE).size());
            assertEquals(1, player.getCompatibleEquipmentsByCategory(unit, EquipmentCategory.FIREARM).size());
        }
    }
}
