package com.mg.nmlonline.domain.model.user;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AuthResponse {
    private String token;
    private Long id;
    private String name;

    public AuthResponse(String token, Long id, String name) {
        this.token = token;
        this.id = id;
        this.name = name;
    }

}
