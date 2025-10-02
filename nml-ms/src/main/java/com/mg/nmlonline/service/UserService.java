package com.mg.nmlonline.service;

import com.mg.nmlonline.entity.user.User;
import com.mg.nmlonline.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

}
