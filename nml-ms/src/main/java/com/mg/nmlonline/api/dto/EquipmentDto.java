package com.mg.nmlonline.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentDto {
    private String name;
    private double cost;
    private double pdfBonus;
    private double pdcBonus;
    private double armBonus;
    private double evasionBonus;
    private Set<UnitClassDto> compatibleClass;
    private String category;
}
