package com.mg.nmlonline.domain.model.building;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Énumération des types de bâtiments avec leurs caractéristiques.
 */
@Getter
@AllArgsConstructor
public enum BuildingType {
    /**
     * Quartier Général - Centre névralgique de l'empire
     * Stats : 100 Atk / 200 Def
     * Déplacement : tous les 5 tours
     * Stocke 25% de la fortune
     */
    HEADQUARTERS("Quartier Général", "QG", 100, 200, 5),

    /**
     * Cache d'armes - Stockage des équipements
     * Stats : 100 Atk / 100 Def
     * Déplacement : tous les tours (cooldown = 0)
     * Capacité max : 300 équipements
     */
    WEAPON_CACHE("Cache d'armes", "CA", 100, 100, 0),

    /**
     * Banque - Stockage de l'argent et des ressources
     * Stats : 50 Atk / 50 Def
     * Déplacement : une seule fois, à partir du tour 5
     * Stocke 75% de la fortune
     */
    BANK("Banque", "BQ", 50, 50, -1); // -1 indique un déplacement unique

    private final String displayName;
    private final String code;
    private final double baseAttack;
    private final double baseDefense;
    private final int moveCooldown; // -1 = déplacement unique
}

