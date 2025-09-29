package com.mg.nmlonline.controller;

import com.mg.nmlonline.service.JwtService;
import com.mg.nmlonline.model.player.AuthResponse;
import com.mg.nmlonline.model.player.LoginRequest;
import com.mg.nmlonline.model.player.User;
import com.mg.nmlonline.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {
    @Autowired
    private UserService userService;
    @Autowired
    private JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        User user = userService.findByUsername(req.getUsername());
        if (user != null && userService.checkPassword(req.getPassword(), user.getPassword())) {
            String token = jwtService.generateToken(user);
            return ResponseEntity.ok(new AuthResponse(token, user.getId(), user.getUsername(), user.getMoney()));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Login failed");
    }
}