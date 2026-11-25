package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.user.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("JwtService — claims, types et expiration")
class JwtServiceTest {

    private final JwtService service = new JwtService("0123456789abcdef0123456789abcdef");

    private static User user(String role) {
        User user = new User();
        user.setId(7L);
        user.setUsername("lurio");
        user.setRole(role);
        return user;
    }

    @Test
    @DisplayName("Un access token porte id, nom, rôle ; rôle absent → USER")
    void accessTokenExposesClaims() {
        JwtService.JwtClaims claims = service.validateAndExtractClaims(
                service.generateToken(user("ADMIN"), 60_000));

        assertEquals(7L, claims.userId());
        assertEquals("lurio", claims.username());
        assertEquals("ADMIN", claims.role());
        assertEquals("USER", service.validateAndExtractClaims(
                service.generateToken(user(null), 60_000)).role());
    }

    @Test
    @DisplayName("Un refresh token n'est jamais accepté comme access token")
    void refreshTokenIsRejectedAsAccessToken() {
        JwtService.RefreshToken refresh = service.generateRefreshToken(user("USER"), 60_000);

        assertNull(service.validateAndExtractClaims(refresh.token()));
        assertEquals(refresh.jti(), service.extractJti(refresh.token()));
        assertEquals(7L, service.extractRefreshUserId(refresh.token()));
        assertNull(service.extractJti(service.generateToken(user("USER"), 60_000)),
                "Un access token n'a pas de JTI de refresh");
    }

    @Test
    @DisplayName("Token expiré ou falsifié rejeté")
    void invalidTokensAreRejected() {
        String expired = service.generateToken(user("USER"), -1_000);
        assertThrows(ExpiredJwtException.class, () -> service.validateAndExtractClaims(expired));

        String valid = service.generateToken(user("USER"), 60_000);
        String forged = valid.substring(0, valid.length() - 2) + "xy";
        assertThrows(JwtException.class, () -> service.validateAndExtractClaims(forged));
    }

    @Test
    @DisplayName("Un secret de moins de 32 caractères est refusé au démarrage")
    void shortSecretIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new JwtService("trop-court"));
        assertNotNull(service);
    }
}
