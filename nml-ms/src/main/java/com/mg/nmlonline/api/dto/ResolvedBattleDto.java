package com.mg.nmlonline.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class ResolvedBattleDto {
    private int sectorNumber;
    private boolean standoff;
    private List<StandoffParticipantDto> participants;
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
    private int capturedBuildings;
    private boolean attackerCharacterLost;
    private boolean defenderCharacterLost;
    private boolean defenderHeadquartersCaptured;
    private List<BattleLogEntryDto> battleLog;

    @Data
    public static class BattleLogEntryDto {
        private String phase;
        private String outcome;
        private String message;
    }

    @Data
    public static class StandoffParticipantDto {
        private Long playerId;
        private String playerName;
        private int casualties;
        private int injured;
        private boolean characterLost;
        private boolean eliminated;
    }
}
