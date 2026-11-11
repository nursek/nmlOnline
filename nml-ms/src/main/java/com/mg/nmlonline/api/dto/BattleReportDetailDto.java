package com.mg.nmlonline.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class BattleReportDetailDto {
    private int schemaVersion;
    private String kind;
    private int turn;
    private int sectorNumber;
    private boolean standoff;
    private boolean ranged;
    private String rangedFire;
    private Long winnerPlayerId;
    private String winnerName;
    private int capturedBuildings;
    private List<BattleReportDto.CampDto> camps;
    private List<BattleReportDto.CasualtyDto> casualties;
    private List<BattleReportDto.ExperienceGainDto> experienceGains;
    private List<BattleReportDto.PlayerForceDto> forces;
    private List<LogEntryDto> battleLog;

    @Data
    public static class LogEntryDto {
        private String phase;
        private String outcome;
        private String message;
    }
}
