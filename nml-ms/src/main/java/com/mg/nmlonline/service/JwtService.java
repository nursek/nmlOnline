package com.mg.nmlonline.service;

import com.mg.nmlonline.model.player.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Service
public class JwtService {
    private final String SECRET = "secret-key-very-long-for-hmac-sha256-1234567890"; // À remplacer par une vraie clé en prod
    private final long EXPIRATION = 86400000; // 1 jour

    public String generateToken(User user) {
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("id", user.getId())
                .claim("name", user.getUsername())
                .claim("money", user.getMoney())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}