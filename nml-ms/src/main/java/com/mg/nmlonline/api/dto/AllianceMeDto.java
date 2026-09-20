package com.mg.nmlonline.api.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AllianceMeDto {
    private boolean headquartersOperational;
    private List<AllianceDto> alliances = new ArrayList<>();
    private List<AllianceProposalDto> incomingProposals = new ArrayList<>();
    private List<AllianceProposalDto> outgoingProposals = new ArrayList<>();
    private List<AlliedPlayerDto> alliedPlayers = new ArrayList<>();
    private List<BetrayalBonusDto> activeBetrayalBonuses = new ArrayList<>();

    @Data
    public static class AllianceDto {
        private Long id;
        private Long allyPlayerId;
        private String allyName;
        private int createdTurn;
    }

    @Data
    public static class AlliedPlayerDto {
        private Long playerId;
        private String name;
    }

    @Data
    public static class BetrayalBonusDto {
        private Long victimPlayerId;
        private String victimName;
        private double bonusPercent;
    }
}
