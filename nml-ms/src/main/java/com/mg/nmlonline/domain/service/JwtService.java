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
import java.util.UUID;

/**
 * Service de gestion des tokens JWT.
 * Génère, valide et extrait les informations des tokens.
 */
@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey key;

    public JwtService(@Value("${jwt.secret}") String secret) {
        // La clé doit faire au moins 32 caractères (256 bits) pour HS256.
        // On refuse de démarrer avec un secret trop faible plutôt que de le compléter
        // avec des caractères prévisibles.
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException(
                "jwt.secret must be at least 32 characters long (256 bits) for HS256");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Record contenant les informations extraites du token JWT.
     */
    public record JwtClaims(Long userId, String username, String role) {}

    /**
     * Record contenant les informations d'un refresh token.
     */
    public record RefreshToken(String token, String jti, long expiry) {}

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
                .claim("role", user.getRole() != null ? user.getRole() : "USER")
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    /**
     * Génère un refresh token avec un JTI unique et sa date d'expiration.
     *
     * @param user L'utilisateur pour lequel générer le refresh token
     * @param expirationMillis Durée de validité en millisecondes
     * @return Le refresh token et ses métadonnées
     */
    public RefreshToken generateRefreshToken(User user, long expirationMillis) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMillis);
        String jti = UUID.randomUUID().toString();

        String token = Jwts.builder()
                .subject(user.getUsername())
                .claim("id", user.getId())
                .claim("jti", jti)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();

        return new RefreshToken(token, jti, expiration.getTime());
    }

    /**
     * Extrait le JTI d'un refresh token après vérification de la signature.
     * Utilisé pour la recherche indexée avant vérification du hash.
     *
     * @param token Le refresh token
     * @return Le JTI, ou null s'il est absent ou invalide
     */
    public String extractJti(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.get("jti", String.class);
        } catch (JwtException e) {
            logger.debug("Cannot extract JTI from token: {}", e.getMessage());
            return null;
        }
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
            String role = claims.get("role", String.class);

            if (userId == null || username == null || username.isBlank()) {
                logger.warn("Token missing required claims");
                return null;
            }

            return new JwtClaims(userId, username, role != null ? role : "USER");

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
