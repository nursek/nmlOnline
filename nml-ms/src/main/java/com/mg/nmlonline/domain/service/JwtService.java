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

@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey key;

    public JwtService(@Value("${jwt.secret}") String secret) {
        // HS256 exige au moins 256 bits ; on refuse un secret faible plutôt que de le compléter prévisiblement.
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException(
                "jwt.secret must be at least 32 characters long (256 bits) for HS256");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public record JwtClaims(Long userId, String username, String role) {}

    public record RefreshToken(String token, String jti, long expiry) {}

    public String generateToken(User user, long expirationMillis) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("id", user.getId())
                .claim("name", user.getUsername())
                .claim("role", user.getRole() != null ? user.getRole() : "USER")
                .claim("type", "access")
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

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

    /** Extrait le JTI (signature vérifiée) pour recherche indexée avant vérification du hash. */
    public String extractJti(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!"refresh".equals(claims.get("type", String.class))) {
                return null;
            }
            return claims.get("jti", String.class);
        } catch (JwtException e) {
            logger.debug("Cannot extract JTI from token: {}", e.getClass().getSimpleName());
            return null;
        }
    }

    /** Gson (sérialiseur de jjwt-gson) rend les entiers en Double, que Claims ne convertit pas en Long. */
    private static Long claimAsLong(Claims claims, String name) {
        Number value = claims.get(name, Number.class);
        return value == null ? null : value.longValue();
    }

    /** Id du propriétaire d'un refresh token signé, même si son JTI n'est plus stocké (réutilisation). */
    public Long extractRefreshUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!"refresh".equals(claims.get("type", String.class))) {
                return null;
            }
            return claimAsLong(claims, "id");
        } catch (JwtException e) {
            logger.debug("Cannot extract user id from refresh token: {}", e.getClass().getSimpleName());
            return null;
        }
    }

    public JwtClaims validateAndExtractClaims(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Interdit l'usage d'un refresh token comme access token (type confusion).
            if (!"access".equals(claims.get("type", String.class))) {
                logger.warn("Token rejected: not an access token");
                return null;
            }

            Long userId = claimAsLong(claims, "id");
            String username = claims.getSubject();
            String role = claims.get("role", String.class);

            if (userId == null || username == null || username.isBlank()) {
                logger.warn("Token missing required claims");
                return null;
            }

            return new JwtClaims(userId, username, role != null ? role : "USER");

        } catch (ExpiredJwtException e) {
            logger.debug("Token expired");
            throw e;
        } catch (SignatureException e) {
            logger.warn("Invalid JWT signature");
            throw e;
        } catch (MalformedJwtException e) {
            logger.warn("Malformed JWT");
            throw e;
        } catch (Exception e) {
            logger.warn("JWT validation error: {}", e.getClass().getSimpleName());
            throw new JwtException("Invalid token", e);
        }
    }
}
