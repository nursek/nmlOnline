package com.mg.nmlonline.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class BattleReportDto {
    private int turn;
    private int sectorNumber;
    private String kind;
    private boolean standoff;
    private boolean ranged;
    private String rangedFire;
    private Long winnerPlayerId;
    private String winnerName;
    private int capturedBuildings;
    private List<CampDto> camps;
    private List<CasualtyDto> casualties;
    private List<ExperienceGainDto> experienceGains;
    private List<PlayerForceDto> forces;
    private List<BattleHighlightDto> highlights;
    private Long viewerPlayerId;

    @Data
    public static class CampDto {
        private Long playerId;
        private String playerName;
        private boolean eliminated;
        private boolean characterLost;
        private boolean playerEliminated;
        private String eliminationCause;
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

    @Data
    public static class PlayerForceDto {
        private Long playerId;
        private String playerName;
        private String side;
        private List<UnitForceDto> characters;
        private List<VehicleForceDto> vehicles;
        private List<UnitForceDto> infantry;
        private List<BuildingForceDto> buildings;
        private StatsDto totals;
    }

    @Data
    public static class UnitForceDto {
        private Integer number;
        private String type;
        private String displayName;
        private Double experience;
        private boolean injured;
        private String classCode;
        private List<String> equipmentNames;
        private StatsDto stats;
    }

    @Data
    public static class VehicleForceDto {
        private String displayName;
        private boolean aerial;
        private List<UnitForceDto> crew;
        private StatsDto stats;
    }

    @Data
    public static class BuildingForceDto {
        private String displayName;
        private String category;
        private StatsDto stats;
    }

    @Data
    public static class StatsDto {
        private double attack;
        private double pdf;
        private double pdc;
        private double defense;
        private double armor;
        private double evasion;
    }
}
