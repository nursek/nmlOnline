package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.user.User;
import com.mg.nmlonline.infrastructure.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

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

    public boolean checkPassword(String raw, String hashed) {
        // Schéma actuel : mot de passe + pepper
        if (encoder.matches(applyPepper(raw), hashed)) {
            return true;
        }
        // Rétro-compatibilité : anciens hashes encodés sans pepper.
        // Nécessaire après le refactoring qui a introduit le pepper, pour ne pas
        // invalider les comptes existants tant qu'ils ne sont pas re-hashés.
        return encoder.matches(raw, hashed);
    }

    /**
     * Vérifie le mot de passe et, si le hash stocké est un hash legacy sans pepper qui matche,
     * re-hashe immédiatement avec le pepper et persiste le hash upgradé.
     * Évite qu'un dump DB laisse les comptes pré-refactor craquables offline sans le pepper.
     * À supprimer une fois tous les comptes legacy re-hashés.
     * ponytail: ceiling = transparent re-hash on login ; une fois la migration one-shot faite,
     * retirer la branche legacy + ce upgrade.
     */
    public boolean checkAndUpgradePassword(User user, String raw) {
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

    /**
     * Trouve un utilisateur par son refresh token via une recherche indexée sur le JTI,
     * puis vérifie le hash stocké.
     */
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
