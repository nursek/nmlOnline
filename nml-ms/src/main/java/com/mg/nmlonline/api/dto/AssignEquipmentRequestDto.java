package com.mg.nmlonline.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AssignEquipmentRequestDto {
    @NotBlank
    private String equipmentName;
}