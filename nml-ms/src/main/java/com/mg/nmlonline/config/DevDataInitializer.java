package com.mg.nmlonline.config;

import com.mg.nmlonline.domain.model.user.User;
import com.mg.nmlonline.domain.service.UserService;
import com.mg.nmlonline.infrastructure.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.LinkedHashMap;
import java.util.Map;

/** Comptes de dev (profil {@code dev}). Mots de passe/rôles ré-appliqués à chaque boot pour survivre à un changement de {@code jwt.pepper} ou du schéma d'encodage. */
@Slf4j
@Component
@Profile("dev")
public class DevDataInitializer {

    private final UserRepository userRepository;
    private final UserService userService;

    private final Map<String, String> devAccounts = new LinkedHashMap<>();

    public DevDataInitializer(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;

        devAccounts.put("cegorach", "USER");
        devAccounts.put("mortarion", "USER");
        devAccounts.put("angron", "USER");
        devAccounts.put("lurio", "USER");
        devAccounts.put("nursek", "USER");
        devAccounts.put("admin", "ADMIN");
    }

    @PostConstruct
    public void init() {
        log.warn("[DEV ONLY] Vérification/mise à jour des comptes de développement");

        int created = 0;
        int updated = 0;

        for (Map.Entry<String, String> entry : devAccounts.entrySet()) {
            String username = entry.getKey();
            String role = entry.getValue();

            User user = userRepository.findByUsername(username);
            if (user == null) {
                createUser(username, username, role);
                created++;
            } else {
                user.setPassword(userService.encodePassword(username));
                user.setRole(role);
                userRepository.save(user);
                updated++;
            }
        }

        log.info("[DEV ONLY] {} compte(s) créé(s), {} compte(s) mis à jour",
                created, updated);
    }

    private void createUser(String username, String password, String role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(userService.encodePassword(password));
        user.setRole(role);
        userRepository.save(user);
    }
}
