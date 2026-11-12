package com.mg.nmlonline.domain.model.sniper;

import com.mg.nmlonline.domain.model.building.Building;
import com.mg.nmlonline.domain.model.unit.CombatEntity;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.vehicle.Vehicle;

public final class SniperRules {

    private SniperRules() {
    }

    public static int hitMalus(CombatEntity target) {
        if (target instanceof Unit unit) {
            if (unit.getType() == null) {
                return 0;
            }
            int base = switch (unit.getType()) {
                case LARBIN -> 20;
                case VOYOU -> 35;
                case MALFRAT -> 50;
                case BRUTE -> 65;
                case PERSONNAGE -> 35;
            };
            return clamp(base + (int) Math.round(unit.getEnemyAimMalus()));
        }
        if (target instanceof Vehicle) {
            return 25;
        }
        if (target instanceof Building) {
            return 0;
        }
        if (target instanceof GameCharacter) {
            return 35;
        }
        return 0;
    }

    public static boolean hits(Unit shooter, CombatEntity target, int jet) {
        if (target instanceof Vehicle vehicle && vehicle.isAerial()) {
            int chance = (int) Math.round(shooter.getAerialHitChance() * 100);
            return jet <= chance;
        }
        int malus = clamp(hitMalus(target) - (int) Math.round(shooter.getAimMalusReduction()));
        return jet > malus;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
