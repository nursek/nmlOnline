package com.mg.nmlonline.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AdminTemplatePlayerRequestDto(
        @NotBlank @Size(max = 50) String name,
        @NotBlank String race,
        @NotNull @Size(min = 1, max = 3) List<Integer> sectorNumbers,
        @Size(max = 100) String characterName) {
}
