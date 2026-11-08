package com.mg.nmlonline.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CreateSniperOrderRequestDto {
    @NotNull
    @Positive
    private Integer targetSectorNumber;
}
