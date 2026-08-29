package com.mg.nmlonline.config;

import com.mg.nmlonline.domain.model.user.User;
import com.mg.nmlonline.domain.service.UserService;
import com.mg.nmlonline.infrastructure.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * S'assure qu'en prod l'application démarre avec un compte admin
 * (username {@code admin}, password via {@code ADMIN_PASSWORD}).
 * <p>
 * Le mot de passe est ré-appliqué à chaque boot pour suivre une rotation de
 * {@code ADMIN_PASSWORD} sans intervention DB. Hash bcrypt+pepper Java → impossible
 * en migration Flyway, donc exécuté après le démarrage du contexte.
 */
@Slf4j
@Component
@Profile("prod")
public class ProdAdminInitializer {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_ROLE = "ADMIN";

    private final UserRepository userRepository;
    private final UserService userService;
    private final String adminPassword;

    public ProdAdminInitializer(
            UserRepository userRepository,
            UserService userService,
            @Value("${app.admin.password}") String adminPassword
    ) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.adminPassword = adminPassword;
    }

    @PostConstruct
    public void init() {
        if (adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD absent : impossible de démarrer en prod sans mot de passe admin.");
        }

        User user = userRepository.findByUsername(ADMIN_USERNAME);
        if (user == null) {
            createAdmin();
            log.info("[PROD] Compte admin '{}' créé depuis ADMIN_PASSWORD.", ADMIN_USERNAME);
        } else {
            user.setPassword(userService.encodePassword(adminPassword));
            user.setRole(ADMIN_ROLE);
            userRepository.save(user);
            log.info("[PROD] Mot de passe admin '{}' ré-appliqué depuis ADMIN_PASSWORD.", ADMIN_USERNAME);
        }
    }

    private void createAdmin() {
        User user = new User();
        user.setUsername(ADMIN_USERNAME);
        user.setPassword(userService.encodePassword(adminPassword));
        user.setRole(ADMIN_ROLE);
        userRepository.save(user);
    }
}