package com.mg.nmlonline.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class RankingsDto {
    private int turn;
    private List<RankingEntryDto> military;
    private List<RankingEntryDto> economic;
}
