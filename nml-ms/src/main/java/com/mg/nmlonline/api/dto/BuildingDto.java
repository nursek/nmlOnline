package com.mg.nmlonline.api.dto;

import lombok.Data;
import java.util.List;

/**
 * DTO pour les bâtiments (QG, Cache d'armes, Banque).
 */
@Data
public class BuildingDto {
    private Long id;
    private Long playerId;
    private String buildingType; // HEADQUARTERS, WEAPON_CACHE, BANK
    private String displayName;

    // Stats de combat
    private Double attack;
    private Double defense;

    // État
    private Boolean isDestroyed;
    private Boolean isCaptured;
    private Long capturedByPlayerId;
    private Integer capturedTurn;

    // Déplacement
    private Integer lastMovedTurn;
    private Boolean canMove; // Calculé selon le type et les règles
    private Integer moveCooldown;

    // Localisation
    private Integer sectorNumber;

    // === Spécifique au QG ===
    private Boolean isOperational;
    private Double storedWealth; // 25% de la fortune

    // === Spécifique à la Cache d'armes ===
    private Integer maxCapacity;
    private Integer currentCapacity;
    private Integer availableCapacity;
    private Double fillPercentage;
    private List<EquipmentStackDto> storedEquipments;

    // === Spécifique à la Banque ===
    private Boolean hasMoved;
    private Double storedMoney; // 75% de la fortune
    private Double currentVampirizeRate;
    private List<PlayerResourceDto> storedResources;
}

