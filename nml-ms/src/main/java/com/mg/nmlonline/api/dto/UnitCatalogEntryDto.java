package com.mg.nmlonline.api.dto;

import lombok.Data;

@Data
public class UnitCatalogEntryDto {
    private String name;
    private int cost;
    private int baseAttack;
    private int baseDefense;
    private int availableFromTurn;
    private int maxPerTurn;
    private int purchasedThisTurn;
    private boolean availableNow;
}
