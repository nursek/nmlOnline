package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);

    /**
     * Trouve tous les utilisateurs ayant un refresh token actif (non expiré).
     * Optimisation : évite de charger tous les utilisateurs.
     */
    @Query("SELECT u FROM User u WHERE u.refreshTokenHash IS NOT NULL AND u.refreshTokenExpiry > :currentTime")
    List<User> findAllWithActiveRefreshToken(@Param("currentTime") long currentTime);

    /**
     * Variante sans paramètre, utilise le temps actuel via défaut.
     */
    default List<User> findAllWithActiveRefreshToken() {
        return findAllWithActiveRefreshToken(System.currentTimeMillis());
    }
}