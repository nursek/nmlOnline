package com.mg.nmlonline.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminCharacterRequestDto {
    @NotBlank
    @Size(max = 100)
    private String name;

    @PositiveOrZero
    private double baseAttack;

    @PositiveOrZero
    private double baseDefense;

    @PositiveOrZero
    private double basePdf;

    @PositiveOrZero
    private double basePdc;

    @PositiveOrZero
    private double baseArmor;

    @PositiveOrZero
    private double baseEvasion;
}
