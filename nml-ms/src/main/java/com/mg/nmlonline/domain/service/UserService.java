package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.user.User;
import com.mg.nmlonline.infrastructure.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private static final String DUMMY_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private static final Pattern AVATAR_DATA_URL =
            Pattern.compile("^data:image/(png|jpeg|webp);base64,([A-Za-z0-9+/]+={0,2})$");
    private static final int MAX_AVATAR_BYTES = 512 * 1024;

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final String pepper;

    public UserService(
            UserRepository userRepo,
            PasswordEncoder encoder,
            JwtService jwtService,
            @Value("${jwt.pepper}") String pepper
    ) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.pepper = pepper;
    }

    public User findByUsername(String username) {
        return userRepo.findByUsername(username);
    }

    public Optional<User> findById(Long userId) {
        return userRepo.findById(userId);
    }

    /**
     * Re-hashe et persiste avec pepper si le hash stocké est un legacy sans pepper qui matche.
     * À supprimer une fois tous les comptes legacy re-hashés.
     */
    public boolean checkAndUpgradePassword(User user, String raw) {
        if (user == null) {
            // Utilisateur inconnu : comparer quand même à un hash factice pour un temps de réponse constant (anti-énumération).
            encoder.matches(applyPepper(raw), DUMMY_HASH);
            // Second calcul : un compte existant en fait deux (repli legacy), l'écart de durée trahirait son existence.
            encoder.matches(raw, DUMMY_HASH);
            return false;
        }
        String hashed = user.getPassword();
        if (encoder.matches(applyPepper(raw), hashed)) {
            return true;
        }
        if (encoder.matches(raw, hashed)) {
            user.setPassword(encodePassword(raw));
            userRepo.save(user);
            logger.info("Hash de mot de passe upgradé avec pepper pour l'utilisateur '{}'", user.getUsername());
            return true;
        }
        return false;
    }

    public String encodePassword(String raw) {
        return encoder.encode(applyPepper(raw));
    }

    @Transactional
    public User updateUsername(Long userId, String username) {
        User user = requireUser(userId);
        String pseudo = username == null ? "" : username.trim();
        if (pseudo.length() < 2) {
            throw new IllegalArgumentException("Le pseudo doit faire au moins 2 caractères");
        }
        if (pseudo.equals(user.getUsername())) {
            return user;
        }
        if (userRepo.findByUsername(pseudo) != null) {
            throw new IllegalArgumentException("Ce pseudo est déjà utilisé");
        }
        user.setUsername(pseudo);
        try {
            return userRepo.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Deux changements simultanés vers le même pseudo : l'index unique tranche.
            throw new IllegalArgumentException("Ce pseudo est déjà utilisé");
        }
    }

    @Transactional
    public void updatePassword(Long userId, String currentPassword, String newPassword) {
        User user = requireUser(userId);
        if (!checkAndUpgradePassword(user, currentPassword)) {
            throw new IllegalArgumentException("Mot de passe actuel incorrect");
        }
        user.setPassword(encodePassword(newPassword));
        userRepo.save(user);
        logger.info("Mot de passe modifié pour l'utilisateur {}", user.getUsername());
    }

    @Transactional
    public User updateAvatar(Long userId, String avatarUrl) {
        User user = requireUser(userId);
        user.setAvatarUrl(validateAvatar(avatarUrl));
        return userRepo.save(user);
    }

    private User requireUser(Long userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Compte introuvable"));
    }

    private static String validateAvatar(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            return null;
        }
        String valeur = avatarUrl.trim();
        if (valeur.length() > 1_000_000) {
            throw new IllegalArgumentException("Photo trop lourde (512 Ko maximum)");
        }
        Matcher matcher = AVATAR_DATA_URL.matcher(valeur);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Format de photo non supporté (PNG, JPEG ou WebP)");
        }
        byte[] octets;
        try {
            octets = Base64.getDecoder().decode(matcher.group(2));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Photo invalide");
        }
        if (octets.length == 0 || octets.length > MAX_AVATAR_BYTES) {
            throw new IllegalArgumentException("Photo trop lourde (512 Ko maximum)");
        }
        if (!estImageSupportee(octets)) {
            throw new IllegalArgumentException("Le contenu de la photo ne correspond pas au format annoncé");
        }
        return valeur;
    }

    // Le type MIME d'une data URL est déclaratif : on vérifie les magic bytes.
    private static boolean estImageSupportee(byte[] octets) {
        boolean png = octets.length >= 8
                && octets[0] == (byte) 0x89 && octets[1] == 'P' && octets[2] == 'N' && octets[3] == 'G';
        boolean jpeg = octets.length >= 3
                && octets[0] == (byte) 0xFF && octets[1] == (byte) 0xD8 && octets[2] == (byte) 0xFF;
        boolean webp = octets.length >= 12
                && octets[0] == 'R' && octets[1] == 'I' && octets[2] == 'F' && octets[3] == 'F'
                && octets[8] == 'W' && octets[9] == 'E' && octets[10] == 'B' && octets[11] == 'P';
        return png || jpeg || webp;
    }

    private String applyPepper(String raw) {
        return raw + pepper;
    }

    public void save(User user) {
        userRepo.save(user);
    }

    public void saveRefreshToken(User user, String refreshTokenHash, String refreshTokenJti, Long refreshTokenExpiry) {
        user.setRefreshTokenHash(refreshTokenHash);
        user.setRefreshTokenJti(refreshTokenJti);
        user.setRefreshTokenExpiry(refreshTokenExpiry);
        userRepo.save(user);
    }

    public void resetRefreshToken(User user) {
        user.setRefreshTokenHash(null);
        user.setRefreshTokenJti(null);
        user.setRefreshTokenExpiry(null);
        userRepo.save(user);
    }

    /** Recherche indexée sur le JTI puis vérification du hash stocké. */
    public User findByRefreshToken(String refreshToken) {
        String jti = jwtService.extractJti(refreshToken);
        if (jti == null) {
            return null;
        }

        String transformed = hashInput(refreshToken);
        User user = userRepo.findByRefreshTokenJti(jti).orElse(null);
        if (user == null) {
            return null;
        }

        String hash = user.getRefreshTokenHash();
        if (hash != null && encoder.matches(transformed, hash)) {
            return user;
        }
        return null;
    }

    private String hashInput(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((value + pepper).getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("Hashing algorithm not available", e);
        }
    }

}
