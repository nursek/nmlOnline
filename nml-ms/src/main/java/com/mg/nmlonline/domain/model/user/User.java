package com.mg.nmlonline.domain.model.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "CREDENTIALS", indexes = {
    @Index(name = "idx_refresh_token_jti", columnList = "refreshTokenJti")
})
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
    @SequenceGenerator(name = "user_seq", sequenceName = "credentials_id_seq", allocationSize = 50)
    private Long id;
    private String username;
    private String password;
    private String refreshTokenHash;
    private Long refreshTokenExpiry;
    private String refreshTokenJti;
    private String role;
}
