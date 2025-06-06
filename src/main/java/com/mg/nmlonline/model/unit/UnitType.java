package com.mg.nmlonline.model.unit;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Énumération pour les types d'unités
@Getter
@AllArgsConstructor
public enum UnitType {
    LARBIN(1, 0, 1, 10, 10, 1, 1, 1),
    VOYOU(2, 2, 4, 20, 20, 1, 1, 2),
    MALFRAT(3, 5, 7, 50, 50, 1, 2, 3),
    BRUTE(4, 8, Integer.MAX_VALUE, 100, 100, 1, 3, 4);

    private final int level;
    private final int minExp;
    private final int maxExp;
    private final int baseAttack;
    private final int baseDefense;
    private final int maxFirearms;
    private final int maxMeleeWeapons;
    private final int maxDefensiveEquipment;

    public static UnitType getTypeByExperience(int experience) {
        if (experience >= 8) return BRUTE;
        if (experience >= 5) return MALFRAT;
        if (experience >= 2) return VOYOU;
        return LARBIN;
    }
}
