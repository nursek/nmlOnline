package com.mg.nmlonline.api.dto;

import lombok.Data;

@Data
public class SectorStatsDto {
    private Double totalAtk;
    private Double totalPdf;
    private Double totalPdc;
    private Double totalDef;
    private Double totalArmor;
    private Double totalOffensive;
    private Double totalDefensive;
    private Double globalStats;
}