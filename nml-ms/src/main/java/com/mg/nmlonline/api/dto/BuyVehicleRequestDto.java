package com.mg.nmlonline.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Corps de la requête d'achat d'un véhicule.
 */
@Data
@NoArgsConstructor
public class BuyVehicleRequestDto {
    @NotBlank(message = "Le type de véhicule est requis")
    private String vehicleType;

    @Min(value = 1, message = "La quantité doit être d'au moins 1")
    private int quantity = 1;
}
