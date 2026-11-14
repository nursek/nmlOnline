package com.mg.nmlonline.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record DistributeUnitsRequestDto(
        @Min(1) int sectorNumber,
        @Min(1) int quantity,
        @NotEmpty List<String> classes) {
}
