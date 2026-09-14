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
    private int transitCombats;
    private List<SectorCaptureDto> capturedSectors;
    private String message;
}