package com.mg.nmlonline.domain.model.unit;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UnitType {
    LARBIN(1, 0, 1, 10, 10, 1, 1, 1, 400, 2),
    VOYOU(2, 2, 4, 20, 20, 1, 1, 2, 1500, 5),
    MALFRAT(3, 5, 7, 50, 50, 1, 2, 3, 5000, 10),
    BRUTE(4, 8, Integer.MAX_VALUE, 100, 100, 1, 3, 4, 0, 0),

    PERSONNAGE(0, -1, -1, 0, 0, 0, 0, 0, 0, 0); // Expérience fixe -1 (sentinelle)


    private final int level;
    private final int minExp;
    private final int maxExp;
    private final int baseAttack;
    private final int baseDefense;
    private final int maxFirearms;
    private final int maxMeleeWeapons;
    private final int maxDefensiveEquipment;
    private final int cost;
    private final int availableFromTurn;

    public static UnitType getTypeByExperience(double experience) {
        if (experience >= 8) return BRUTE;
        if (experience >= 5) return MALFRAT;
        if (experience >= 2) return VOYOU;
        return LARBIN;
    }

    public boolean isPurchasable() {
        return cost > 0;
    }

    public boolean isAvailableAt(int turn) {
        return isPurchasable() && turn >= availableFromTurn;
    }

    /** Plafond par tour : LARBIN et VOYOU augmentent aux tours 5 et 10. */
    public int maxPerTurnAt(int turn) {
        return switch (this) {
            case LARBIN -> turn >= 10 ? 40 : turn >= 5 ? 30 : 20;
            case VOYOU -> turn >= 10 ? 10 : 5;
            case MALFRAT -> 5;
            case BRUTE, PERSONNAGE -> 0;
        };
    }
}
