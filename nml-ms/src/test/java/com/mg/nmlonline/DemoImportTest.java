package com.mg.nmlonline;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@EmbeddedPostgresTest
@DisplayName("Import démo — les parties importées passent par le démarrage officiel")
class DemoImportTest {

    private static final List<String> MAIN_PLAYERS = List.of(
            "lurio", "nursek", "mortarion", "angron", "cegorach", "imotekh", "trazyn");
    private static final List<String> ARCS_PLAYERS = List.of(
            "lurio", "nursek", "mortarion", "angron", "cegorach", "imotekh");

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Test
    @DisplayName("les deux cartes de démo sont démarrées avec des comptes reliés")
    void demoBoardsAreStartedWithLinkedAccounts() {
        for (String name : List.of(DemoFixtures.MAIN_BOARD_NAME, DemoFixtures.ARCS_BOARD_NAME)) {
            Board board = boardRepository.findByName(name)
                    .orElseThrow(() -> new AssertionError("Plateau de démo absent : " + name));
            assertFalse(board.isPreparation(), "Plateau de démo resté en tour 0 : " + name);
            assertNotNull(board.getStartedAt(), "Plateau de démo sans started_at : " + name);

            List<Player> players = playerRepository.findByBoardId(board.getId());
            assertFalse(players.isEmpty(), "Plateau de démo sans joueur : " + name);
            Map<String, Player> playersByName = players.stream()
                    .collect(Collectors.toMap(Player::getName, player -> player));
            List<String> expectedPlayers =
                    DemoFixtures.MAIN_BOARD_NAME.equals(name) ? MAIN_PLAYERS : ARCS_PLAYERS;
            for (String playerName : expectedPlayers) {
                Player player = playersByName.get(playerName);
                assertNotNull(player, "Joueur de démo absent : " + playerName);
                assertNotNull(player.getUserId(), "Compte démo non relié : " + playerName);
            }
        }
    }
}
