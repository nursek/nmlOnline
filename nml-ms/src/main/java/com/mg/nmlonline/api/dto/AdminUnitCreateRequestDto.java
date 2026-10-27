package com.mg.nmlonline.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class AdminUnitCreateRequestDto {
    @NotBlank(message = "le type d'unité est requis")
    private String type;

    private List<String> classes = List.of();

    @Min(value = 1, message = "la quantité doit être au moins 1")
    private int quantity = 1;

    private Integer sectorNumber;
}
