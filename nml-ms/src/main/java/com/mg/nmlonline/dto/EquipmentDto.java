package com.mg.nmlonline.dto;

import com.mg.nmlonline.entity.unit.UnitClass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentDto {
    private Long id;
    private String name;
    private int cost;
    private int pdfBonus;
    private int pdcBonus;
    private int armBonus;
    private int evasionBonus;
    private UnitClass compatibleClass;
    private String category;
}
