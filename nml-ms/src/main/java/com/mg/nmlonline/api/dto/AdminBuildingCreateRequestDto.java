package com.mg.nmlonline.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminBuildingCreateRequestDto {
    @NotBlank(message = "le type de bâtiment est requis")
    private String buildingType;

    private Integer sectorNumber;
}
