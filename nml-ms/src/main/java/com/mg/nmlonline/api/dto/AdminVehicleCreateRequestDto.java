package com.mg.nmlonline.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminVehicleCreateRequestDto {
    @NotBlank(message = "le type de véhicule est requis")
    private String vehicleType;

    @Min(value = 1, message = "la quantité doit être au moins 1")
    private int quantity = 1;

    private Integer sectorNumber;
}
