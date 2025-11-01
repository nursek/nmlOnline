package com.mg.nmlonline.api.dto;

import lombok.Data;
import java.util.List;

@Data
public class SectorDto {
    private Integer number;
    private String name;
    private Double income;
    private List<UnitDto> army;
    private SectorStatsDto stats;

    // Nouvelles propriétés pour la carte
    private Long ownerId;
    private String color;
    private String resource;
    private List<Integer> neighbors;
}