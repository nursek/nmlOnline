package com.mg.nmlonline.controller;

import com.mg.nmlonline.service.JwtService;
import com.mg.nmlonline.entity.user.AuthResponse;
import com.mg.nmlonline.entity.user.LoginRequest;
import com.mg.nmlonline.entity.user.User;
import com.mg.nmlonline.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.util.WebUtils;
import jakarta.servlet.http.Cookie;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@RestController
@RequestMapping("/api")
public class AuthController {
    @Autowired
    private UserService userService;
    @Autowired
    private JwtService jwtService;

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final long BLOCK_TIME_MS = TimeUnit.MINUTES.toMillis(1);

    @Value("${app.cookie.secure:false}")
    private boolean appCookieSecure;

    private static final String PEPPER = "un-secret-tres-long-a-mettre-en-env";
    private static final int BCRYPT_COST = 12;
    private static final long ACCESS_TOKEN_EXPIRATION = 10 * 60 * 1000; // 10 min

    private static class Attempt {
        int count;
        long lastAttempt;
        long blockedUntil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpServletRequest request, HttpServletResponse response) {
        String key = request.getRemoteAddr() + ":" + req.getUsername();
        Attempt att = attempts.computeIfAbsent(key, k -> new Attempt());
        long now = System.currentTimeMillis();

        if (att.blockedUntil > now) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Trop de tentatives, réessayez plus tard");
        }

        User user = userService.findByUsername(req.getUsername());
        boolean valid = user != null && userService.checkPassword(req.getPassword(), user.getPassword());
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        if (valid) {
            att.count = 0;
            String accessToken = jwtService.generateToken(user, ACCESS_TOKEN_EXPIRATION);
            String refreshToken = generateRefreshToken();
            String refreshTokenHash = hash(refreshToken);
            userService.saveRefreshToken(user, refreshTokenHash);
            Cookie cookie = new Cookie("refresh_token", refreshToken);
            cookie.setHttpOnly(true);
            cookie.setPath("/api/auth/refresh");
            cookie.setMaxAge(7 * 24 * 60 * 60);
            cookie.setSecure(appCookieSecure);
            cookie.setAttribute("SameSite", "Lax");
            response.addCookie(cookie);
            return ResponseEntity.ok(new AuthResponse(accessToken, user.getId(), user.getUsername(), user.getMoney()));
        } else {
            att.count++;
            att.lastAttempt = now;
            if (att.count >= MAX_ATTEMPTS) {
                att.blockedUntil = now + BLOCK_TIME_MS;
                att.count = 0;
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Identifiants invalides");
        }
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = WebUtils.getCookie(request, "refresh_token");
        if (cookie == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{}");
        String refreshToken = cookie.getValue();
        User user = userService.findByRefreshToken(refreshToken);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{}");

        String newRefreshToken = generateRefreshToken();
        String newRefreshTokenHash = hash(newRefreshToken);
        userService.saveRefreshToken(user, newRefreshTokenHash);
        Cookie newCookie = new Cookie("refresh_token", newRefreshToken);
        newCookie.setHttpOnly(true);
        newCookie.setPath("/api/auth/refresh");
        newCookie.setMaxAge(7 * 24 * 60 * 60);
        newCookie.setSecure(appCookieSecure);
        newCookie.setAttribute("SameSite", "Lax");
        response.addCookie(newCookie);

        String accessToken = jwtService.generateToken(user, ACCESS_TOKEN_EXPIRATION);
        return ResponseEntity.ok(new AuthResponse(accessToken, user.getId(), user.getUsername(), user.getMoney()));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody LoginRequest req) {
        if (userService.findByUsername(req.getUsername()) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Utilisateur déjà existant");
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(userService.encodePassword(req.getPassword()));
        user.setMoney(100);
        userService.save(user);
        return ResponseEntity.ok("Utilisateur créé");
    }

    // Helpers
    private String generateRefreshToken() {
        byte[] bytes = new byte[64];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((value + PEPPER).getBytes());
            return new BCryptPasswordEncoder(BCRYPT_COST).encode(Base64.getEncoder().encodeToString(hash));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
