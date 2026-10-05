package com.mg.nmlonline.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BuyUnitRequestDto {
    @NotBlank(message = "Le type d'unité est requis")
    private String unitType;

    @NotBlank(message = "La classe est requise")
    private String unitClass;

    @Min(value = 1, message = "La quantité doit être d'au moins 1")
    private int quantity = 1;
}
