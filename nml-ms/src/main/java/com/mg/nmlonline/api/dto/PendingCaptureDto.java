package com.mg.nmlonline.api.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PendingCaptureDto {
    private Long id;
    private Long boardId;
    private int sectorNumber;
    private int turn;
    private List<CandidateDto> candidates = new ArrayList<>();

    @Data
    public static class CandidateDto {
        private Long playerId;
        private String playerName;
    }
}
