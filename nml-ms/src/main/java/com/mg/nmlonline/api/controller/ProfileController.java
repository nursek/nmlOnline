package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.ProfileDto;
import com.mg.nmlonline.api.dto.UpdateAvatarDto;
import com.mg.nmlonline.api.dto.UpdatePasswordDto;
import com.mg.nmlonline.api.dto.UpdateUsernameDto;
import com.mg.nmlonline.domain.model.user.User;
import com.mg.nmlonline.domain.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping("/username")
    public ProfileDto updateUsername(@Valid @RequestBody UpdateUsernameDto request,
                                     HttpServletRequest httpRequest) {
        return toDto(userService.updateUsername(userId(httpRequest), request.username()));
    }

    @PutMapping("/avatar")
    public ProfileDto updateAvatar(@RequestBody UpdateAvatarDto request,
                                   HttpServletRequest httpRequest) {
        return toDto(userService.updateAvatar(userId(httpRequest), request.avatarUrl()));
    }

    @PutMapping("/password")
    public ResponseEntity<Void> updatePassword(@Valid @RequestBody UpdatePasswordDto request,
                                               HttpServletRequest httpRequest) {
        userService.updatePassword(userId(httpRequest), request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    private static Long userId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (!(userId instanceof Long id)) {
            throw new SecurityException("Utilisateur non authentifié");
        }
        return id;
    }

    private static ProfileDto toDto(User user) {
        return new ProfileDto(user.getId(), user.getUsername(), user.getRole(), user.getAvatarUrl());
    }
}
