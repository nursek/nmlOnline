package com.mg.nmlonline.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class TurnJournalDto {
    private int turn;
    private String exportedAt;
    private boolean finalized;
    private SummaryDto summary;
    private List<ResolvedBattleDto> battles;
    private List<BattleReportDetailDto> rangedCombats;
    private List<SectorCaptureDto> captures;
    private List<TransitCombatResultDto> transitCombats;
    private List<String> eliminatedPlayers;
    private boolean gameFinished;
    private Long winnerPlayerId;
    private String winnerName;
    private boolean winnerPosthumous;
    private boolean winnerAdjusted;
    private Integer gazetteDraftTurn;

    @Data
    public static class SummaryDto {
        private int battles;
        private int rangedCombats;
        private int resolvedOrders;
        private int blockedOrders;
        private int captures;
        private int eliminatedPlayers;
        private int transitCombats;
    }
}
