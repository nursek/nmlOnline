package com.mg.nmlonline.api.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Corps de la requête d'achat d'un véhicule.
 */
@Data
@NoArgsConstructor
public class BuyVehicleRequestDto {
    private String vehicleType;
    private int quantity = 1;
}
