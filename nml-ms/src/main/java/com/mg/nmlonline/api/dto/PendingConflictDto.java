package com.mg.nmlonline.api.dto;

import lombok.Data;

/**
 * Conflit du hop courant en attente de résolution admin ; conflictId cible l'appel resolve-battle.
 */
@Data
public class PendingConflictDto {
    private int conflictId;
    private int sectorNumber;
    private Long attackerPlayerId;
    private String attackerName;
    private Long defenderPlayerId;
    private String defenderName;
}