package com.mg.nmlonline.api.dto;

import lombok.Data;

/**
 * Conflit à la destination du hop courant, en attente de résolution manuelle
 * par l'admin. {@code conflictId} identifie le conflit pour cibler l'appel
 * {@code resolve-battle}.
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