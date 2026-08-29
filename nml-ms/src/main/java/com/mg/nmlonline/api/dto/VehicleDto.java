package com.mg.nmlonline.api.dto;

import lombok.Data;

@Data
public class VehicleDto {
    private Long id;
    private Long playerId;
    private String vehicleType;
    private String displayName;

    private Double pdf;
    private Double defense;

    private Boolean isDestroyed;

    private Integer speed;
    private Integer capacity;
    private Integer passengerCount;
    private Boolean hasPilot;

    private Integer sectorNumber;
    private Long boardId;
}
