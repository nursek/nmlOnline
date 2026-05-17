package com.mg.nmlonline.api.dto;

import lombok.Data;

/**
 * DTO pour les véhicules.
 */
@Data
public class VehicleDto {
    private Long id;
    private Long playerId;
    private String vehicleType;
    private String displayName;

    // Stats de combat
    private Double pdf;
    private Double defense;

    // État
    private Boolean isDestroyed;

    // Mobilité
    private Integer speed;
    private Integer capacity;
    private Integer passengerCount;
    private Boolean hasPilot;

    // Localisation
    private Integer sectorNumber;
}
