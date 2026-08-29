package com.mg.nmlonline.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Ordre à pied : un ou plusieurs CombatEntity partageant la même route.
 */
@Data
public class PlaceFootOrderRequestDto {
    @NotEmpty
    private List<@NotNull Long> entityIds;

    @NotEmpty
    private List<@NotNull Integer> route;
}