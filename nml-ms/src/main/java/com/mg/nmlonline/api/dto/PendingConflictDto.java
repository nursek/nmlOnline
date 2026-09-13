package com.mg.nmlonline.api.dto;

import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Conflit du hop courant en attente de résolution admin ; conflictId cible l'appel resolve-battle.
 * Les champs attacker/defender ne sont renseignés que pour un duel à 2 camps.
 */
@Data
public class PendingConflictDto {
    private int conflictId;
    private int sectorNumber;
    private boolean standoff;
    private List<ParticipantDto> participants;
    private Long attackerPlayerId;
    private String attackerName;
    private Long defenderPlayerId;
    private String defenderName;

    @Data
    public static class ParticipantDto {
        private Long playerId;
        private String playerName;
        private Instant submittedAt;
    }
}
