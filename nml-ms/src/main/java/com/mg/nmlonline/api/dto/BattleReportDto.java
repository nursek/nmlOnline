package com.mg.nmlonline.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class BattleReportDto {
    private int turn;
    private int sectorNumber;
    private boolean standoff;
    private Long winnerPlayerId;
    private String winnerName;
    private int capturedBuildings;
    private List<CampDto> camps;
    private List<CasualtyDto> casualties;
    private List<ExperienceGainDto> experienceGains;

    @Data
    public static class CampDto {
        private Long playerId;
        private String playerName;
        private boolean eliminated;
        private boolean characterLost;
    }

    @Data
    public static class CasualtyDto {
        private Long playerId;
        private String playerName;
        private String label;
        private String category;
        private String unitType;
        private Integer unitNumber;
        private Double experience;
    }

    @Data
    public static class ExperienceGainDto {
        private Long playerId;
        private String playerName;
        private int unitNumber;
        private String typeBefore;
        private double experienceBefore;
        private double gained;
        private String typeAfter;
        private double experienceAfter;
    }
}
