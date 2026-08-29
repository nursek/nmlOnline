package com.mg.nmlonline.domain.model.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Requête de connexion — validation volontairement légère pour accepter les anciens comptes et les comptes de dev (ex: a/a) ; l'inscription stricte passe par RegisterRequest. */
@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 1, max = 50, message = "Username must be between 1 and 50 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 1, max = 100, message = "Password must be between 1 and 100 characters")
    private String password;

    private boolean rememberMe;

}
