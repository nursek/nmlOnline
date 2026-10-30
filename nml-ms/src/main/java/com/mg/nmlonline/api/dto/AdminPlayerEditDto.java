package com.mg.nmlonline.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class AdminPlayerEditDto {
    private PlayerDto player;
    private List<UnitDto> units;
    private List<VehicleDto> vehicles;
}
