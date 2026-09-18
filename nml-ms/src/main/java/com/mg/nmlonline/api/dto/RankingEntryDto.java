package com.mg.nmlonline.api.dto;

import lombok.Data;

@Data
public class RankingEntryDto {
    private Long playerId;
    private String name;
    private double power;
    private String comment;
}
