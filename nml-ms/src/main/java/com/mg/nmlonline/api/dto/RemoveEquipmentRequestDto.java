package com.mg.nmlonline.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RemoveEquipmentRequestDto {
    @NotBlank
    private String equipmentName;
}