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

    private List<BuildingDto> buildings;
    private GameCharacterDto character;
    private List<VehicleDto> vehicles;

    private Long ownerId;
    private Long boardId;
    private String color;
    private String resource;
    private List<Integer> neighbors;

    private Integer x;
    private Integer y;
}