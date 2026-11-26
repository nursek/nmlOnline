package com.mg.nmlonline;

import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.user.User;
import com.mg.nmlonline.domain.service.BoardService;
import com.mg.nmlonline.domain.service.PlayerService;
import com.mg.nmlonline.domain.service.JwtService;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class DemoFixtures {

    public static final String MAIN_BOARD_NAME = "Carte Principale";
    public static final String ARCS_BOARD_NAME = "Carte 6 Joueurs (Arcs)";
    public static final long TOKEN_TTL_MS = 3_600_000L;

    private DemoFixtures() {
    }

    public static Long mainBoardId(BoardRepository boardRepository) {
        return boardRepository.findByName(MAIN_BOARD_NAME)
                .orElseThrow(() -> new IllegalStateException("Plateau de démo principal introuvable"))
                .getId();
    }

    public static String tokenFor(User user, JwtService jwtService) {
        return jwtService.generateToken(user, TOKEN_TTL_MS);
    }

    public static String tokenFor(UserRepository userRepository, JwtService jwtService, String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalStateException("Utilisateur de test introuvable : " + username);
        }
        return tokenFor(user, jwtService);
    }

    public static String bearer(UserRepository userRepository, JwtService jwtService, String username) {
        return "Bearer " + tokenFor(userRepository, jwtService, username);
    }

    public static Player playerOfTestUser(UserRepository userRepository, PlayerService playerService,
                                          BoardService boardService, String username) {
        User user = userRepository.findByUsername(username);
        assertNotNull(user, "L'utilisateur de test " + username + " doit exister");
        Player player = playerService.findByUserId(user.getId(),
                boardService.getAllBoards().getFirst().getId());
        assertNotNull(player, "Le joueur lié à " + username + " doit exister");
        return player;
    }
}
