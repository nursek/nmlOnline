package com.mg.nmlonline.api.dto;

import lombok.Data;

@Data
public class TransitCombatResultDto {
    private int sectorNumber;
    private Long vehicleId;
    private boolean vehicleFired;
}