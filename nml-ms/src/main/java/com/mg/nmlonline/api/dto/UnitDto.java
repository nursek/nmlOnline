package com.mg.nmlonline.api.dto;

import lombok.Data;
import java.util.List;

@Data
public class UnitDto {
    private Integer id;
    private Long playerId;
    private Integer number;
    private String name; // Nom optionnel pour les personnages
    private Double experience;
    private UnitTypeDto type;
    private List<UnitClassDto> classes;
    private Boolean isInjured;
    private List<EquipmentDto> equipments;

    private Double attack;
    private Double defense;
    private Double pdf;
    private Double pdc;
    private Double armor;
    private Double evasion;
}