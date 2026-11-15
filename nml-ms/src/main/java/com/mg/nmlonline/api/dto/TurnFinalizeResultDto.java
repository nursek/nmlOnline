package com.mg.nmlonline.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class TurnFinalizeResultDto {
    private int newTurn;
    private int turnEnding;
    private int resolvedOrders;
    private int blockedOrders;
    private int conflictsResolved;
    private int transitCombatsCount;
    private List<TransitCombatResultDto> transitCombats;
    private List<SectorCaptureDto> capturedSectors;
    private List<String> eliminatedPlayers;
    private boolean gameFinished;
    private Long winnerPlayerId;
    private String winnerName;
    private boolean winnerPosthumous;
    private boolean winnerAdjusted;
    private Integer gazetteDraftTurn;
    private String message;
}