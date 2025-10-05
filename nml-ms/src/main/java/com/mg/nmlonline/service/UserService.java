package com.mg.nmlonline.service;

import com.mg.nmlonline.entity.user.User;
import com.mg.nmlonline.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepo;

    private PasswordEncoder encoder = new BCryptPasswordEncoder();

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

    public User findByRefreshToken(String refreshToken) {
        String transformed = hashInput(refreshToken);
        for (User user : userRepo.findAll()) {
            String hash = user.getRefreshTokenHash();
            if (hash != null && encoder.matches(transformed, hash)) {
                return user;
            }
        }
        return null;
    }

    private String hashInput(String value) {
        try {
            String PEPPER = "un-secret-tres-long-a-mettre-en-env";
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((value + PEPPER).getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

}
