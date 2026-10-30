package com.mg.nmlonline.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminPlayerNameRequestDto {
    @NotBlank
    @Size(max = 100)
    private String name;
}
