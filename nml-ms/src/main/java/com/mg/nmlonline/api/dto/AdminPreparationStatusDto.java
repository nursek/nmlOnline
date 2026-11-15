package com.mg.nmlonline.api.dto;

public record AdminPreparationStatusDto(
        Long playerId,
        String name,
        boolean accountLinked,
        int sectorCount,
        boolean characterPlaced,
        int buildingsPlaced,
        int buildingsTotal,
        int unitsTotal,
        int unitsUnclassified,
        boolean ready) {
}
