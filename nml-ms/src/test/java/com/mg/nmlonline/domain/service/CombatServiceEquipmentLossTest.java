package com.mg.nmlonline.domain.service;

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
}
