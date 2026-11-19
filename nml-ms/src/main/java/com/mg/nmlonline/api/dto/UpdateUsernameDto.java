package com.mg.nmlonline.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUsernameDto(
        @NotBlank(message = "Le pseudo est requis")
        @Size(min = 2, max = 50, message = "Le pseudo doit faire entre 2 et 50 caractères")
        String username) {
}
