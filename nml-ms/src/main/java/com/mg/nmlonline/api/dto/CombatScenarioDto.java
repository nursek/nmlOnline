package com.mg.nmlonline.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class CombatScenarioDto {
    private String code;
    private String label;
    private String description;
    private List<String> observations;
    private int arenaSector;
    private int stagingSector;
}
