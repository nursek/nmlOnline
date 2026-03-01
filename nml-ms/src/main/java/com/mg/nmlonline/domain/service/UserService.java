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
    private final String pepper;

    public UserService(
            UserRepository userRepo,
            PasswordEncoder encoder,
            @Value("${jwt.pepper}") String pepper
    ) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.pepper = pepper;
    }

    public User findByUsername(String username) {
        return userRepo.findByUsername(username);
    }

    public boolean checkPassword(String raw, String hashed) {
        return encoder.matches(raw, hashed);
    }

    public String encodePassword(String raw) {
        return encoder.encode(raw);
    }

    public void save(User user) {
        userRepo.save(user);
    }

    public void saveRefreshToken(User user, String refreshTokenHash) {
        user.setRefreshTokenHash(refreshTokenHash);
        userRepo.save(user);
    }

    public void resetRefreshToken(User user) {
        user.setRefreshTokenHash(null);
        user.setRefreshTokenExpiry(null);
        userRepo.save(user);
    }

    /**
     * Trouve un utilisateur par son refresh token.
     * Utilise une recherche directe par hash SHA-256+pepper (O(1) en DB).
     */
    public User findByRefreshToken(String refreshToken) {
        String hash = hashInput(refreshToken);
        return userRepo.findByRefreshTokenHash(hash).orElse(null);
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
