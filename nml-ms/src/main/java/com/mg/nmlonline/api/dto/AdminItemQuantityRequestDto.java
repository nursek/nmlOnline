package com.mg.nmlonline.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminItemQuantityRequestDto {
    @NotBlank(message = "le nom est requis")
    private String name;

    @Min(value = 1, message = "la quantité doit être au moins 1")
    private int quantity = 1;
}
