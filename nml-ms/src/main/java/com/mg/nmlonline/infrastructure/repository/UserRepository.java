package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
}