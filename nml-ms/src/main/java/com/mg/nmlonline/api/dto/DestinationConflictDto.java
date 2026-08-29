package com.mg.nmlonline.api.dto;

import lombok.Data;

@Data
public class DestinationConflictDto {
    private int sectorNumber;
    private Long attackerPlayerId;
    private String attackerName;
    private Long defenderPlayerId;
    private String defenderName;
}