package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);

    Optional<User> findByRefreshTokenJti(String refreshTokenJti);
}