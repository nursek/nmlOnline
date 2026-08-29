package com.mg.nmlonline.api.dto;

import lombok.Data;
import java.util.List;

@Data
public class BuildingDto {
    private Long id;
    private Long playerId;
    private String buildingType; // HEADQUARTERS, WEAPON_CACHE, BANK
    private String displayName;

    private Double attack;
    private Double defense;

    private Boolean isDestroyed;
    private Boolean isCaptured;
    private Long capturedByPlayerId;
    private Integer capturedTurn;

    private Integer lastMovedTurn;
    private Boolean canMove;
    private Integer moveCooldown;

    private Integer sectorNumber;

    private Boolean isOperational;
    private Double storedWealth; // 25% de la fortune

    private Integer maxCapacity;
    private Integer currentCapacity;
    private Integer availableCapacity;
    private Double fillPercentage;
    private List<EquipmentStackDto> storedEquipments;

    private Boolean hasMoved;
    private Double storedMoney; // 75% de la fortune
    private Double currentVampirizeRate;
    private List<PlayerResourceDto> storedResources;
}

