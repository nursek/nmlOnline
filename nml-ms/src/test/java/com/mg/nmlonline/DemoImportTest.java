package com.mg.nmlonline;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@EmbeddedPostgresTest
@DisplayName("Import démo — les parties importées passent par le démarrage officiel")
class DemoImportTest {

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
            for (Player player : players) {
                assertNotNull(player.getUserId(), "Compte démo non relié : " + player.getName());
            }
        }
    }
}
