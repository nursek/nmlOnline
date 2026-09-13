package com.mg.nmlonline.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PlaceVehicleOrderRequestDto {
    @NotEmpty
    private List<@NotNull Integer> route;
}
