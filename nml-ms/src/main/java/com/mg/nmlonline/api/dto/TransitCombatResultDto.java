package com.mg.nmlonline.api.dto;

import lombok.Data;

/**
 * DTO d'un combat de transit (mirror du record
 * {@link com.mg.nmlonline.domain.model.movement.TransitCombatResult}).
 */
@Data
public class TransitCombatResultDto {
    private int sectorNumber;
    private Long vehicleId;
    private boolean vehicleFired;
}