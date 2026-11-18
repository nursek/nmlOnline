package com.mg.nmlonline.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePasswordDto(
        @NotBlank(message = "Le mot de passe actuel est requis")
        String currentPassword,

        @NotBlank(message = "Le nouveau mot de passe est requis")
        @Size(min = 8, max = 100, message = "Le mot de passe doit faire entre 8 et 100 caractères")
        String newPassword) {
}
