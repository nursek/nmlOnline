package com.mg.nmlonline.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminUnitEquipRequestDto {
    @NotBlank
    private String name;
}
