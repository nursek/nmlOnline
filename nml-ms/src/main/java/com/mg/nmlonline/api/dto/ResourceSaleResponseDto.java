package com.mg.nmlonline.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceSaleResponseDto {
    private String message;
    private double saleValue;
    private String resourceName;
    private int quantitySold;
}

