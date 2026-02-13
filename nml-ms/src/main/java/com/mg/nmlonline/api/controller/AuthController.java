package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.domain.service.JwtService;
import com.mg.nmlonline.domain.model.user.AuthResponse;
import com.mg.nmlonline.domain.model.user.LoginRequest;
import com.mg.nmlonline.domain.model.user.User;
import com.mg.nmlonline.domain.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.util.WebUtils;
import jakarta.servlet.http.Cookie;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final String pepper;
    private final boolean appCookieSecure;

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, RefreshThrottle> refreshThrottles = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final long BLOCK_TIME_MS = TimeUnit.MINUTES.toMillis(1);
    private static final long REFRESH_MIN_INTERVAL_MS = 1000; // 1 seconde minimum entre chaque refresh
    private static final long GRACE_PERIOD_MS = 3000; // 3 secondes de grâce pour les requêtes en doublon (spam F5)
    private static final long ACCESS_TOKEN_EXPIRATION = 10 * 60 * 1000L; // 10 min

    public AuthController(
            UserService userService,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            @Value("${jwt.pepper:default-pepper-secret-change-in-production}") String pepper,
            @Value("${app.cookie.secure:false}") boolean appCookieSecure
    ) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.pepper = pepper;
        this.appCookieSecure = appCookieSecure;
    }

    private static class Attempt {
        int count;
        long lastAttempt;
        long blockedUntil;
    }

    /**
     * Stocke l'état du throttling pour le refresh token.
     * Inclut une "grace period" pour gérer le spam F5 :
     * - Si on reçoit le même token (ou l'ancien) dans les 3 secondes, on renvoie le même résultat
     * - Cela évite d'invalider le token si plusieurs requêtes arrivent en parallèle
     */
    private static class RefreshThrottle {
        long lastRefresh;
        int refreshCount;
        String lastToken;      // Le nouveau token généré
        String previousToken;  // L'ancien token (celui reçu)
        String newToken;       // Le nouveau token à renvoyer
        Map<String, Object> lastResponse;  // La réponse à renvoyer pendant la grace period
        int lastCookieMaxAge;  // MaxAge du cookie
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

            // Durée selon rememberMe
            long refreshTokenDurationMs = req.isRememberMe() ? (30L * 24 * 60 * 60 * 1000) : (24L * 60 * 60 * 1000);
            long refreshTokenExpiry = now + refreshTokenDurationMs;
            user.setRefreshTokenExpiry(refreshTokenExpiry);
            userService.saveRefreshToken(user, refreshTokenHash);

            int refreshTokenMaxAge = (int) (refreshTokenDurationMs / 1000);
            Cookie cookie = new Cookie("refresh_token", refreshToken);
            cookie.setHttpOnly(true);
            cookie.setPath("/api/auth");
            cookie.setMaxAge(refreshTokenMaxAge);
            cookie.setSecure(appCookieSecure);
            cookie.setAttribute("SameSite", "Lax");
            response.addCookie(cookie);
            return ResponseEntity.ok(new AuthResponse(accessToken, user.getId(), user.getUsername()));
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
        String clientIp = request.getRemoteAddr();
        long now = System.currentTimeMillis();

        // Protection anti-spam : vérifier le throttling par IP AVANT toute opération
        RefreshThrottle throttle = refreshThrottles.computeIfAbsent(clientIp, k -> new RefreshThrottle());

        // Vérifier le cookie AVANT toute autre opération
        Cookie cookie = WebUtils.getCookie(request, "refresh_token");
        if (cookie == null) {
            return ResponseEntity.ok(Map.of("valid", false));
        }

        String refreshToken = cookie.getValue();

        // GRACE PERIOD : Si on vient de faire un refresh récemment avec le MÊME token,
        // retourner le même résultat sans re-générer (évite les problèmes de spam F5)
        if (throttle.lastRefresh > 0 && throttle.lastToken != null) {
            long timeSinceLastRefresh = now - throttle.lastRefresh;

            // Fenêtre de grâce de 3 secondes
            if (timeSinceLastRefresh < GRACE_PERIOD_MS) {
                // Vérifier si c'est le même token ou l'ancien token
                if (refreshToken.equals(throttle.lastToken) || refreshToken.equals(throttle.previousToken)) {
                    // Retourner le résultat précédent (le nouveau token déjà généré)
                    if (throttle.lastResponse != null) {
                        // Renvoyer le même cookie avec le nouveau token
                        Cookie sameCookie = new Cookie("refresh_token", throttle.newToken);
                        sameCookie.setHttpOnly(true);
                        sameCookie.setPath("/api/auth");
                        sameCookie.setMaxAge(throttle.lastCookieMaxAge);
                        sameCookie.setSecure(appCookieSecure);
                        sameCookie.setAttribute("SameSite", "Lax");
                        response.addCookie(sameCookie);
                        return ResponseEntity.ok(throttle.lastResponse);
                    }
                }
            }

            // Hors grace period, vérifier le rate limiting
            if (timeSinceLastRefresh < REFRESH_MIN_INTERVAL_MS) {
                throttle.refreshCount++;
                if (throttle.refreshCount > 5) {
                    logger.warn("Refresh spam detected from IP: {}", clientIp);
                    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                            .body(Map.of("valid", false, "error", "Trop de requêtes, veuillez patienter"));
                }
                // Dans l'intervalle mais pas trop de spam, on continue
            } else {
                // Intervalle respecté, reset le compteur
                throttle.refreshCount = 0;
            }
        }

        // Chercher l'utilisateur par le refresh token
        User user = userService.findByRefreshToken(refreshToken);

        if (user == null || user.getRefreshTokenExpiry() == null || user.getRefreshTokenExpiry() < now) {
            return ResponseEntity.ok(Map.of("valid", false));
        }

        long maxAge = (user.getRefreshTokenExpiry() - now) / 1000;
        if (maxAge <= 0) {
            return ResponseEntity.ok(Map.of("valid", false));
        }

        // Générer le nouveau refresh token
        String newRefreshToken = generateRefreshToken();
        String newRefreshTokenHash = hash(newRefreshToken);
        userService.saveRefreshToken(user, newRefreshTokenHash);

        // Créer le cookie avec le nouveau token
        Cookie newCookie = new Cookie("refresh_token", newRefreshToken);
        newCookie.setHttpOnly(true);
        newCookie.setPath("/api/auth");
        newCookie.setMaxAge((int) maxAge);
        newCookie.setSecure(appCookieSecure);
        newCookie.setAttribute("SameSite", "Lax");
        response.addCookie(newCookie);

        // Générer le nouveau access token
        String accessToken = jwtService.generateToken(user, ACCESS_TOKEN_EXPIRATION);

        // Stocker le résultat pour la grace period
        Map<String, Object> responseData = Map.of(
                "valid", true,
                "token", accessToken,
                "id", user.getId(),
                "name", user.getUsername()
        );

        // Mettre à jour le throttle avec les infos pour la grace period
        throttle.lastRefresh = now;
        throttle.previousToken = refreshToken;  // L'ancien token (celui qu'on vient de recevoir)
        throttle.newToken = newRefreshToken;    // Le nouveau token (celui qu'on envoie)
        throttle.lastToken = newRefreshToken;
        throttle.lastResponse = responseData;
        throttle.lastCookieMaxAge = (int) maxAge;

        return ResponseEntity.ok(responseData);
    }


    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody LoginRequest req) {
        if (userService.findByUsername(req.getUsername()) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Utilisateur déjà existant");
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(userService.encodePassword(req.getPassword()));
        userService.save(user);
        return ResponseEntity.ok("Utilisateur créé");
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        System.out.println("Logout called");
        Cookie cookie = WebUtils.getCookie(request, "refresh_token");
        if (cookie != null) {
            String refreshToken = cookie.getValue();
            User user = userService.findByRefreshToken(refreshToken);
            if (user != null) {
                userService.resetRefreshToken(user);
            }
        }
        Cookie clearCookie = new Cookie("refresh_token", "");
        clearCookie.setHttpOnly(true);
        clearCookie.setPath("/api/auth");
        clearCookie.setMaxAge(0);
        clearCookie.setSecure(appCookieSecure);
        clearCookie.setAttribute("SameSite", "Lax");
        response.addCookie(clearCookie);
        return ResponseEntity.ok().build();
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
            byte[] hash = digest.digest((value + pepper).getBytes());
            return passwordEncoder.encode(Base64.getEncoder().encodeToString(hash));
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("Hashing algorithm not available", e);
        }
    }
}