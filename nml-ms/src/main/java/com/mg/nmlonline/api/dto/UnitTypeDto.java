package com.mg.nmlonline.api.dto;

import lombok.Data;

@Data
public class UnitTypeDto {
    private String name;
    private Integer level;
    private Integer minExp;
    private Integer maxExp;
    private Integer baseAttack;
    private Integer baseDefense;
    private Integer maxFirearms;
    private Integer maxMeleeWeapons;
    private Integer maxDefensiveEquipment;
}