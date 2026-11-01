package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.building.WeaponCache;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentCategory;
import com.mg.nmlonline.domain.model.equipment.EquipmentStack;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CombatService — l'équipement d'une unité détruite quitte l'inventaire")
class CombatServiceEquipmentLossTest {

    private static final Equipment FIREARM = new Equipment(
            "Fusil test", 100, 10, 0, 0, 0, Set.of(UnitClass.LEGER), EquipmentCategory.FIREARM);

    @Test
    @DisplayName("Le total baisse d'un exemplaire, le disponible reste inchangé")
    void discardDestroyedEquipmentDecrementsQuantityOnly() {
        Player owner = new Player("Propriétaire");
        owner.addEquipmentToStack(FIREARM, 2);
        Unit unit = new Unit(0, UnitClass.LEGER);
        assertTrue(unit.addEquipment(FIREARM));
        owner.decrementEquipmentAvailability(FIREARM);

        CombatService.discardDestroyedEquipment(owner, unit);

        EquipmentStack stack = owner.getEquipments().getFirst();
        assertEquals(1, stack.getQuantity());
        assertEquals(1, stack.getAvailable());
    }

    @Test
    @DisplayName("Le dernier exemplaire détruit retire la pile de l'inventaire")
    void discardDestroyedEquipmentRemovesEmptyStack() {
        Player owner = new Player("Propriétaire");
        owner.addEquipmentToStack(FIREARM, 1);
        Unit unit = new Unit(0, UnitClass.LEGER);
        assertTrue(unit.addEquipment(FIREARM));
        owner.decrementEquipmentAvailability(FIREARM);

        CombatService.discardDestroyedEquipment(owner, unit);

        assertTrue(owner.getEquipments().isEmpty());
    }

    @Test
    @DisplayName("L'exemplaire détruit est retiré de la pile qui porte l'équipé, pas de l'inventaire")
    void discardDestroyedEquipmentTargetsTheEquippedStack() {
        Player owner = new Player("Propriétaire");
        owner.addEquipmentToStack(FIREARM, 1);
        // Ids distincts : EquipmentStack.equals ne compare que l'id (deux piles non persistées sont égales).
        owner.getEquipments().getFirst().setId(1L);
        WeaponCache cache = new WeaponCache(1L);
        owner.getBuildings().add(cache);
        EquipmentStack cached = new EquipmentStack(FIREARM);
        cached.setId(2L);
        cached.setQuantity(1);
        cached.setAvailable(0);
        cache.getStoredEquipments().add(cached);
        Unit unit = new Unit(0, UnitClass.LEGER);
        assertTrue(unit.addEquipment(FIREARM));

        CombatService.discardDestroyedEquipment(owner, unit);

        assertEquals(1, owner.getEquipments().getFirst().getQuantity(),
                "L'inventaire disponible ne doit pas être amputé");
        assertTrue(cache.getStoredEquipments().isEmpty());
    }

    @Test
    @DisplayName("Donnée héritée : copie portée sans décompte, l'invariant disponible <= total reste vrai")
    void discardDestroyedEquipmentKeepsInvariantForLegacyUnitEquipment() {
        Player owner = new Player("Propriétaire");
        owner.addEquipmentToStack(FIREARM, 2);
        Unit unit = new Unit(0, UnitClass.LEGER);
        assertTrue(unit.addEquipment(FIREARM));

        CombatService.discardDestroyedEquipment(owner, unit);

        EquipmentStack stack = owner.getEquipments().getFirst();
        assertEquals(1, stack.getQuantity());
        assertEquals(1, stack.getAvailable());
    }
}
