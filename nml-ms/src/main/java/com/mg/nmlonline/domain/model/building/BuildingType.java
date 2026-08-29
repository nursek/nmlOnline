package com.mg.nmlonline.domain.model.building;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BuildingType {
    HEADQUARTERS("Quartier Général", "QG", 100, 200, 5),

    WEAPON_CACHE("Cache d'armes", "CA", 100, 100, 0),

    BANK("Banque", "BQ", 50, 50, -1); // -1 = déplacement unique

    private final String displayName;
    private final String code;
    private final double baseAttack;
    private final double baseDefense;
    private final int moveCooldown; // -1 = déplacement unique
}

