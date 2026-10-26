package com.mg.nmlonline.api.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AdminAllianceHistoryDto {
    private List<AllianceEntryDto> alliances = new ArrayList<>();
    private List<ProposalEntryDto> proposals = new ArrayList<>();

    @Data
    public static class AllianceEntryDto {
        private Long id;
        private Long playerOneId;
        private String playerOneName;
        private Long playerTwoId;
        private String playerTwoName;
        private int createdTurn;
        private Integer endedTurn;
        private Long endedByPlayerId;
        private String endedByName;
        private boolean betrayal;
    }

    @Data
    public static class ProposalEntryDto {
        private Long id;
        private String kind;
        private String status;
        private Long fromPlayerId;
        private String fromPlayerName;
        private Long toPlayerId;
        private String toPlayerName;
        private int createdTurn;
        private Integer resolvedTurn;
    }
}
