package com.mg.nmlonline.api.dto;

import lombok.Data;

/**
 * DTO pour les personnages principaux (leaders de joueurs).
 */
@Data
public class GameCharacterDto {
    private Long id;
    private Long playerId;
    private String name;

    // Stats de base (fixes)
    private Double baseAttack;
    private Double baseDefense;
    private Double basePdf;
    private Double basePdc;
    private Double baseArmor;
    private Double baseEvasion;

    // Localisation
    private Integer sectorNumber;
}

