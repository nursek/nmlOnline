package com.mg.nmlonline.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class SectorConflictDto {
    private int sectorNumber;
    private boolean standoff;
    private List<ParticipantDto> participants;

    @Data
    public static class ParticipantDto {
        private Long playerId;
        private String playerName;
    }
}
