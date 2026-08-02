package com.mg.nmlonline.api.dto;

import lombok.Data;

@Data
public class UnitClassDto {
    private String name;
    private String code;
    // champs additionnels si besoin (ex: criticalChance, criticalMultiplier, damageReduction)
    private Double criticalChance;
    private Double criticalMultiplier;
    private Double damageReductionPdf;
    private Double damageReductionPdc;
    /** Nombre max de secteurs parcourus par tour (LEGER = 2, autres = 1). */
    private Integer maxMovementHops;
}