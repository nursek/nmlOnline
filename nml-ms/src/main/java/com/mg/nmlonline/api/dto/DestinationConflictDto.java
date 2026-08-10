package com.mg.nmlonline.api.dto;

import lombok.Data;

/**
 * DTO d'un conflit à la destination, exposé à l'admin avec les noms résolus
 * des deux joueurs (mirror du record {@link com.mg.nmlonline.domain.model.movement.DestinationConflict}).
 */
@Data
public class DestinationConflictDto {
    private int sectorNumber;
    private Long attackerPlayerId;
    private String attackerName;
    private Long defenderPlayerId;
    private String defenderName;
}