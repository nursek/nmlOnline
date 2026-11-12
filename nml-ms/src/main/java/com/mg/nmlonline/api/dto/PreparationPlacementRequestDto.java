package com.mg.nmlonline.api.dto;

import jakarta.validation.constraints.Min;

public record PreparationPlacementRequestDto(@Min(1) int sectorNumber) {
}
