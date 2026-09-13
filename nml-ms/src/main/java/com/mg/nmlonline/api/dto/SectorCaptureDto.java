package com.mg.nmlonline.api.dto;

import lombok.Data;

@Data
public class SectorCaptureDto {
    private int sectorNumber;
    private Long playerId;
    private String playerName;
    private boolean onTheFly;
}
