package com.mg.nmlonline.domain.model.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Le nom d'utilisateur est obligatoire")
    @Size(min = 2, max = 30, message = "Le nom d'utilisateur doit contenir entre 2 et 30 caractères")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Le nom d'utilisateur ne peut contenir que des lettres, chiffres, tirets et underscores")
    private String username;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, max = 128, message = "Le mot de passe doit contenir entre 6 et 128 caractères")
    private String password;

    private boolean rememberMe;
}
