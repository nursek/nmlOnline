package com.mg.nmlonline.domain.model.battle;

/**
 * Énumération représentant les différents types de batailles possibles.
 */
public enum BattleType {
    /**
     * Pas de bataille (secteur vide ou occupé par un seul joueur)
     */
    NONE,

    /**
     * Simple occupation d'un secteur neutre ou vide (pas de combat)
     */
    OCCUPATION,

    /**
     * Attaque d'un secteur neutre par plusieurs joueurs
     */
    ATTACK,

    /**
     * Défense d'un secteur possédé contre un attaquant
     */
    DEFENSE,

    /**
     * Combat impliquant 3 joueurs ou plus
     */
    MULTI_PLAYER
}

