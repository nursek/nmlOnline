package com.mg.nmlonline.domain.model.sniper;

import com.mg.nmlonline.domain.model.building.Bank;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentCategory;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.domain.model.unit.UnitType;
import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import com.mg.nmlonline.domain.model.vehicle.VehicleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SniperRules — jet de HIT")
class SniperRulesTest {

    private static final Long JOUEUR = 1L;

    private Unit unit(UnitType type, Equipment... equipments) {
        double experience = switch (type) {
            case LARBIN -> 0;
            case VOYOU -> 2;
            case MALFRAT -> 5;
            case BRUTE -> 8;
            case PERSONNAGE -> -1;
        };
        Unit unit = new Unit(experience, UnitClass.SNIPER);
        for (Equipment equipment : equipments) {
            unit.addEquipment(equipment);
        }
        return unit;
    }

    private Equipment firearm(double aimMalusReduction) {
        Equipment equipment = new Equipment("Fusil de test", 0, 0, 0, 0, 0,
                Set.of(UnitClass.SNIPER), EquipmentCategory.FIREARM);
        equipment.setAimMalusReduction(aimMalusReduction);
        return equipment;
    }

    private Equipment defensive(double enemyAimMalus) {
        Equipment equipment = new Equipment("Gilet de test", 0, 0, 0, 0, 0,
                Set.of(UnitClass.SNIPER), EquipmentCategory.DEFENSIVE);
        equipment.setEnemyAimMalus(enemyAimMalus);
        return equipment;
    }

    @Test
    @DisplayName("Malus de HIT par type de cible")
    void hitMalusByTargetType() {
        assertEquals(20, SniperRules.hitMalus(unit(UnitType.LARBIN)));
        assertEquals(35, SniperRules.hitMalus(unit(UnitType.VOYOU)));
        assertEquals(50, SniperRules.hitMalus(unit(UnitType.MALFRAT)));
        assertEquals(65, SniperRules.hitMalus(unit(UnitType.BRUTE)));
        assertEquals(35, SniperRules.hitMalus(unit(UnitType.LARBIN, defensive(15))));
        assertEquals(100, SniperRules.hitMalus(unit(UnitType.BRUTE, defensive(50))));
        assertEquals(25, SniperRules.hitMalus(new Vehicle(VehicleType.TANK, JOUEUR)));
        assertEquals(0, SniperRules.hitMalus(new Bank(JOUEUR)));
        assertEquals(35, SniperRules.hitMalus(new GameCharacter("H", 10, 10, 10, 10, 0, 0)));
    }

    @Test
    @DisplayName("Le fusil réduit le malus à plat ; l'aérien garde la chance du niveau du tireur")
    void hitRollUsesFlatReductionAndAerialChance() {
        Unit shooter = unit(UnitType.LARBIN, firearm(10));
        Unit larbin = unit(UnitType.LARBIN);

        assertEquals(10, Math.round(shooter.getAimMalusReduction()));
        assertFalse(SniperRules.hits(shooter, larbin, 10));
        assertTrue(SniperRules.hits(shooter, larbin, 11));

        Unit sansFusil = unit(UnitType.LARBIN);
        assertFalse(SniperRules.hits(sansFusil, larbin, 20));
        assertTrue(SniperRules.hits(sansFusil, larbin, 21));

        Unit gilet = unit(UnitType.LARBIN, defensive(15));
        assertFalse(SniperRules.hits(sansFusil, gilet, 35));
        assertTrue(SniperRules.hits(sansFusil, gilet, 36));

        assertTrue(SniperRules.hits(sansFusil, new Bank(JOUEUR), 1));
        assertTrue(SniperRules.hits(sansFusil, new Vehicle(VehicleType.HELICOPTERE, JOUEUR), 10));
        assertFalse(SniperRules.hits(sansFusil, new Vehicle(VehicleType.HELICOPTERE, JOUEUR), 11));
    }
}
