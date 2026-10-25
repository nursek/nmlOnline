package com.mg.nmlonline.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class PendingEliminationDto {
    private Long victimPlayerId;
    private String victimPlayerName;
    private String cause;
    private List<ParticipantDto> participants;

    @Data
    public static class ParticipantDto {
        private Long playerId;
        private String playerName;
    }
}
