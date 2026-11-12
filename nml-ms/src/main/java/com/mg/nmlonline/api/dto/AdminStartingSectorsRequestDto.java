package com.mg.nmlonline.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AdminStartingSectorsRequestDto(
        @NotNull @Size(max = 3) List<Integer> sectorNumbers) {
}
