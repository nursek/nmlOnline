package com.mg.nmlonline.domain.model.user;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AuthResponse {
    private String token;
    private Long id;
    private String name;
    private String role;

    public AuthResponse(String token, Long id, String name, String role) {
        this.token = token;
        this.id = id;
        this.name = name;
        this.role = role != null ? role : "USER";
    }

}
