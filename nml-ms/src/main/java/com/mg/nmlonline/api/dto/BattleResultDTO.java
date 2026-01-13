package com.mg.nmlonline.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO pour représenter le résultat d'une bataille côté API REST
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BattleResultDTO {
    /**
     * ID du secteur où la bataille a eu lieu
     */
    private Integer sectorId;

    /**
     * Type de bataille : "ATTACK", "DEFENSE", "MULTI_PLAYER", etc.
     */
    private String battleType;

    /**
     * ID du joueur vainqueur (null si égalité)
     */
    private Long winnerId;

    /**
     * Nombre de pertes par joueur
     * Clé : ID du joueur, Valeur : nombre d'unités perdues
     */
    private Map<Long, Integer> casualties;

    /**
     * Nombre d'unités survivantes par joueur
     * Clé : ID du joueur, Valeur : nombre d'unités restantes
     */
    private Map<Long, Integer> survivors;

    /**
     * Description textuelle du résultat
     */
    private String description;
}

