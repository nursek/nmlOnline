package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.user.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Service de gestion des tokens JWT.
 * Génère, valide et extrait les informations des tokens.
 */
@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey key;

    public JwtService(@Value("${jwt.secret:default-secret-key-very-long-for-hmac-sha256-change-in-prod}") String secret) {
        // S'assurer que la clé fait au moins 32 caractères pour HS256
        if (secret.length() < 32) {
            secret = secret + "0".repeat(32 - secret.length());
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Record contenant les informations extraites du token JWT.
     */
    public record JwtClaims(Long userId, String username) {}

    /**
     * Génère un token JWT pour un utilisateur.
     *
     * @param user L'utilisateur pour lequel générer le token
     * @param expirationMillis Durée de validité en millisecondes
     * @return Le token JWT signé
     */
    public String generateToken(User user, long expirationMillis) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("id", user.getId())
                .claim("name", user.getUsername())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    /**
     * Valide un token JWT et extrait les claims.
     *
     * @param token Le token JWT à valider
     * @return Les claims extraits, ou null si le token est invalide
     * @throws JwtException Si le token est invalide ou expiré
     */
    public JwtClaims validateAndExtractClaims(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Long userId = claims.get("id", Long.class);
            String username = claims.getSubject();

            if (userId == null || username == null || username.isBlank()) {
                logger.warn("Token missing required claims");
                return null;
            }

            return new JwtClaims(userId, username);

        } catch (ExpiredJwtException e) {
            logger.debug("Token expired: {}", e.getMessage());
            throw e;
        } catch (SignatureException e) {
            logger.warn("Invalid JWT signature: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            logger.warn("Malformed JWT: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.warn("JWT validation error: {}", e.getMessage());
            throw new JwtException("Invalid token", e);
        }
    }
}
