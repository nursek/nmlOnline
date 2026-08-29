package com.mg.nmlonline.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PlaceVehicleRequestDto {
    @NotNull(message = "boardId est requis")
    private Long boardId;

    @NotNull(message = "sectorNumber est requis")
    private Integer sectorNumber;
}
