package com.mg.nmlonline.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO pour représenter un ordre de déplacement côté API.
 * Utilisé pour les requêtes envoyées par le client lors de la phase de déplacement.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoveOrderDTO {
    /**
     * ID du joueur qui donne l'ordre
     */
    private Long playerId;

    /**
     * ID du secteur de départ
     */
    private Integer fromSectorId;

    /**
     * ID du secteur de destination
     */
    private Integer toSectorId;

    /**
     * Liste des IDs des unités qui se déplacent
     */
    private List<Integer> unitIds;

    /**
     * Type de déplacement : INTERNAL, NEUTRAL, ENEMY, DOUBLE_MOVE
     */
    private String moveType;

    /**
     * Pour les DOUBLE_MOVE : ID du secteur intermédiaire traversé (optionnel)
     */
    private Integer intermediateSectorId;

    /**
     * Indique si le mouvement a été intercepté (lecture seule, retourné par le serveur)
     */
    private Boolean intercepted;

    /**
     * Indique si le mouvement est instantané (lecture seule, retourné par le serveur)
     */
    private Boolean instant;
}

