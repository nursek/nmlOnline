package com.mg.nmlonline.api.dto;

import lombok.Data;

@Data
public class ResolvedBattleDto {
    private int sectorNumber;
    private Long attackerPlayerId;
    private String attackerName;
    private Long defenderPlayerId;
    private String defenderName;
    private boolean success;
    private String message;
    private Long winnerId;
    private String winnerName;
    private int attackerCasualties;
    private int defenderCasualties;
    private int attackerInjured;
    private int defenderInjured;
}