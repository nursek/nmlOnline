package com.mg.nmlonline.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la réponse d'une vente de ressource
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceSaleResponseDto {
    private String message;
    private double saleValue;
    private String resourceName;
    private int quantitySold;
}

