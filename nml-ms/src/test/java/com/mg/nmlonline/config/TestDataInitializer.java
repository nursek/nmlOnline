package com.mg.nmlonline.config;

import com.mg.nmlonline.domain.model.building.Headquarters;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.user.User;
import com.mg.nmlonline.domain.service.BuildingService;
import com.mg.nmlonline.domain.service.GameCharacterService;
import com.mg.nmlonline.domain.service.UserService;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("test")
@Order(100)
public class TestDataInitializer implements ApplicationRunner {

    // Après PlayerStartupImporter (@Order 2) : les joueurs de test ont besoin du plateau de démo.
    @Override
    public void run(ApplicationArguments args) {
        init();
    }

    public static final String USER_1 = "testuser1";
    public static final String USER_2 = "testuser2";
    public static final String ADMIN = "testadmin";
    public static final String PASSWORD = "password";

    private final UserRepository userRepository;
    private final BoardRepository boardRepository;
    private final UserService userService;
    private final PlayerRepository playerRepository;
    private final GameCharacterService characterService;
    private final BuildingService buildingService;

    public TestDataInitializer(UserRepository userRepository,
                               BoardRepository boardRepository,
                               UserService userService,
                               PlayerRepository playerRepository,
                               GameCharacterService characterService,
                               BuildingService buildingService) {
        this.userRepository = userRepository;
        this.boardRepository = boardRepository;
        this.userService = userService;
        this.playerRepository = playerRepository;
        this.characterService = characterService;
        this.buildingService = buildingService;
    }

    public void init() {
        if (userRepository.findByUsername(USER_1) != null) {
            log.debug("Des utilisateurs existent déjà, skipping TestDataInitializer");
            return;
        }

        log.info("[TEST] Création des données de test");

        User user1 = createUser(USER_1, PASSWORD, "USER");
        User user2 = createUser(USER_2, PASSWORD, "USER");
        createUser(ADMIN, PASSWORD, "ADMIN");

        Long boardId = boardRepository.findAll().getFirst().getId();
        Player player1 = createPlayer("TestPlayer1", user1.getId(), boardId);
        Player player2 = createPlayer("TestPlayer2", user2.getId(), boardId);

        createCharacter(player1.getId(), "HeroOne");
        createCharacter(player2.getId(), "HeroTwo");

        createInitialBuildings(player1);
        createInitialBuildings(player2);

        log.info("[TEST] Données de test créées ({} utilisateurs, {} joueurs)",
                userRepository.count(), playerRepository.count());
    }

    private User createUser(String username, String password, String role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(userService.encodePassword(password));
        user.setRole(role);
        return userRepository.save(user);
    }

    private Player createPlayer(String name, Long userId, Long boardId) {
        Player player = new Player(name);
        player.setUserId(userId);
        player.setBoardId(boardId);
        return playerRepository.save(player);
    }

    private void createCharacter(Long playerId, String name) {
        try {
            characterService.createCharacter(playerId, name, 10, 5, 5, 10, 5, 5);
        } catch (IllegalStateException e) {
            log.warn("Personnage déjà existant pour le joueur {}", playerId);
        }
    }

    private void createInitialBuildings(Player player) {
        buildingService.createInitialBuildings(player);
    }
}
