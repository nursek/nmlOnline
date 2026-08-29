package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.domain.service.JwtService;
import com.mg.nmlonline.domain.model.user.AuthResponse;
import com.mg.nmlonline.domain.model.user.LoginRequest;
import com.mg.nmlonline.domain.model.user.RegisterRequest;
import com.mg.nmlonline.domain.model.user.User;
import com.mg.nmlonline.domain.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.util.WebUtils;
import jakarta.servlet.http.Cookie;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final String pepper;
    private final boolean appCookieSecure;

    private static final int MAX_ATTEMPTS = 5;
    private static final long BLOCK_TIME_MS = TimeUnit.MINUTES.toMillis(1);
    private static final long REFRESH_MIN_INTERVAL_MS = 1000;
    private static final long GRACE_PERIOD_MS = 3000;
    private static final long ACCESS_TOKEN_EXPIRATION = 10 * 60 * 1000L;
    private static final long CLEANUP_INTERVAL_MS = TimeUnit.MINUTES.toMillis(5);
    private static final int MAX_ATTEMPT_ENTRIES = 10_000;
    private static final int MAX_THROTTLE_ENTRIES = 10_000;

    private final Map<String, Attempt> attempts = Collections.synchronizedMap(new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Attempt> eldest) {
            return size() > MAX_ATTEMPT_ENTRIES;
        }
    });
    private final Map<String, RefreshThrottle> refreshThrottles = Collections.synchronizedMap(new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, RefreshThrottle> eldest) {
            return size() > MAX_THROTTLE_ENTRIES;
        }
    });
    private final AtomicLong lastCleanup = new AtomicLong(System.currentTimeMillis());

    public AuthController(
            UserService userService,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            @Value("${jwt.pepper}") String pepper,
            @Value("${app.cookie.secure:true}") boolean appCookieSecure
    ) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.pepper = pepper;
        this.appCookieSecure = appCookieSecure;
    }

    private static class Attempt {
        final AtomicInteger count = new AtomicInteger(0);
        final AtomicLong lastAttempt = new AtomicLong(0);
        final AtomicLong blockedUntil = new AtomicLong(0);
    }

    /**
     * Throttling refresh : grace period qui renvoie le même résultat pour le même token
     * (ou le précédent), évite d'invalider le token sur spam F5 parallèle.
     */
    private static class RefreshThrottle {
        long lastRefresh;
        int refreshCount;
        String lastToken;
        String previousToken;
        String newToken;
        Map<String, Object> lastResponse;
        int lastCookieMaxAge;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request, HttpServletResponse response) {
        long now = System.currentTimeMillis();
        cleanupStaleEntries(now);

        String key = request.getRemoteAddr() + ":" + req.getUsername();
        Attempt att = attempts.computeIfAbsent(key, k -> new Attempt());

        if (att.blockedUntil.get() > now) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Trop de tentatives, réessayez plus tard");
        }

        User user = userService.findByUsername(req.getUsername());
        boolean valid = user != null && userService.checkAndUpgradePassword(user, req.getPassword());

        if (valid) {
            att.count.set(0);
            String accessToken = jwtService.generateToken(user, ACCESS_TOKEN_EXPIRATION);

            long refreshTokenDurationMs = req.isRememberMe() ? (30L * 24 * 60 * 60 * 1000) : (24L * 60 * 60 * 1000);
            JwtService.RefreshToken refresh = jwtService.generateRefreshToken(user, refreshTokenDurationMs);
            String refreshTokenHash = hash(refresh.token());
            userService.saveRefreshToken(user, refreshTokenHash, refresh.jti(), refresh.expiry());

            int refreshTokenMaxAge = (int) (refreshTokenDurationMs / 1000);
            addRefreshCookie(response, refresh.token(), refreshTokenMaxAge);
            return ResponseEntity.ok(new AuthResponse(accessToken, user.getId(), user.getUsername(), user.getRole()));
        } else {
            int currentCount = att.count.incrementAndGet();
            att.lastAttempt.set(now);
            if (currentCount >= MAX_ATTEMPTS) {
                att.blockedUntil.set(now + BLOCK_TIME_MS);
                att.count.set(0);
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants invalides");
        }
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        String clientIp = request.getRemoteAddr();
        long now = System.currentTimeMillis();
        cleanupStaleEntries(now);

        RefreshThrottle throttle = refreshThrottles.computeIfAbsent(clientIp, k -> new RefreshThrottle());

        Cookie cookie = WebUtils.getCookie(request, "refresh_token");
        if (cookie == null) {
            return ResponseEntity.ok(Map.of("valid", false));
        }

        String refreshToken = cookie.getValue();

        if (throttle.lastRefresh > 0 && throttle.lastToken != null) {
            long timeSinceLastRefresh = now - throttle.lastRefresh;

            if (timeSinceLastRefresh < GRACE_PERIOD_MS) {
                if (refreshToken.equals(throttle.lastToken) || refreshToken.equals(throttle.previousToken)) {
                    if (throttle.lastResponse != null) {
                        addRefreshCookie(response, throttle.newToken, throttle.lastCookieMaxAge);
                        return ResponseEntity.ok(throttle.lastResponse);
                    }
                }
            }

            if (timeSinceLastRefresh < REFRESH_MIN_INTERVAL_MS) {
                throttle.refreshCount++;
                if (throttle.refreshCount > 5) {
                    logger.warn("Refresh spam detected from IP: {}", clientIp);
                    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                            .body(Map.of("valid", false, "error", "Trop de requêtes, veuillez patienter"));
                }
            } else {
                throttle.refreshCount = 0;
            }
        }

        User user = userService.findByRefreshToken(refreshToken);

        if (user == null || user.getRefreshTokenExpiry() == null || user.getRefreshTokenExpiry() < now) {
            return ResponseEntity.ok(Map.of("valid", false));
        }

        long maxAge = (user.getRefreshTokenExpiry() - now) / 1000;
        if (maxAge <= 0) {
            return ResponseEntity.ok(Map.of("valid", false));
        }

        JwtService.RefreshToken newRefresh = jwtService.generateRefreshToken(user, user.getRefreshTokenExpiry() - now);
        String newRefreshTokenHash = hash(newRefresh.token());
        userService.saveRefreshToken(user, newRefreshTokenHash, newRefresh.jti(), newRefresh.expiry());

        addRefreshCookie(response, newRefresh.token(), (int) maxAge);

        String accessToken = jwtService.generateToken(user, ACCESS_TOKEN_EXPIRATION);

        Map<String, Object> responseData = Map.of(
                "valid", true,
                "token", accessToken,
                "id", user.getId(),
                "name", user.getUsername(),
                "role", user.getRole() != null ? user.getRole() : "USER"
        );

        throttle.lastRefresh = now;
        throttle.previousToken = refreshToken;
        throttle.newToken = newRefresh.token();
        throttle.lastToken = newRefresh.token();
        throttle.lastResponse = responseData;
        throttle.lastCookieMaxAge = (int) maxAge;

        return ResponseEntity.ok(responseData);
    }


    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (userService.findByUsername(req.getUsername()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Utilisateur déjà existant");
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(userService.encodePassword(req.getPassword()));
        user.setRole("USER");
        userService.save(user);
        return ResponseEntity.ok(Map.of("message", "Utilisateur créé"));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        logger.debug("Logout called");
        Cookie cookie = WebUtils.getCookie(request, "refresh_token");
        if (cookie != null) {
            String refreshToken = cookie.getValue();
            User user = userService.findByRefreshToken(refreshToken);
            if (user != null) {
                userService.resetRefreshToken(user);
            }
        }
        addRefreshCookie(response, "", 0);
        return ResponseEntity.ok().build();
    }

    private void addRefreshCookie(HttpServletResponse response, String value, int maxAge) {
        Cookie cookie = new Cookie("refresh_token", value);
        cookie.setHttpOnly(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(maxAge);
        cookie.setSecure(appCookieSecure);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
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

    private void cleanupStaleEntries(long now) {
        if (now - lastCleanup.get() < CLEANUP_INTERVAL_MS) return;
        lastCleanup.set(now);

        attempts.entrySet().removeIf(e ->
                now - e.getValue().lastAttempt.get() > BLOCK_TIME_MS * 2
                        && e.getValue().blockedUntil.get() < now);

        refreshThrottles.entrySet().removeIf(e ->
                now - e.getValue().lastRefresh > CLEANUP_INTERVAL_MS);
    }
}