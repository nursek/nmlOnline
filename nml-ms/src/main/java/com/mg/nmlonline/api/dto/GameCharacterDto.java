package com.mg.nmlonline.api.dto;

import lombok.Data;

@Data
public class GameCharacterDto {
    private Long id;
    private Long playerId;
    private String name;

    private Double baseAttack;
    private Double baseDefense;
    private Double basePdf;
    private Double basePdc;
    private Double baseArmor;
    private Double baseEvasion;

    private Integer sectorNumber;
}

