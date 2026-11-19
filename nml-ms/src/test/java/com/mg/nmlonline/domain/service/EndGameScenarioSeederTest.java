package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.DemoFixtures;
import com.mg.nmlonline.EmbeddedPostgresTest;
import com.mg.nmlonline.api.dto.GamePodiumDto;
import com.mg.nmlonline.api.dto.NotificationFeedDto;
import com.mg.nmlonline.api.dto.ScenarioSummaryDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.notification.NotificationType;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.press.GazetteIssue;
import com.mg.nmlonline.domain.model.press.GazetteStatus;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.GazetteIssueRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EmbeddedPostgresTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DisplayName("Scénario dev — fin de partie à un survivant")
class EndGameScenarioSeederTest {

    @Autowired
    private EndGameScenarioSeeder seeder;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private GazetteIssueRepository gazetteIssueRepository;

    @Autowired
    private GamePodiumService gamePodiumService;

    @Autowired
    private NotificationService notificationService;

    @Test
    void finishesWithOneSurvivorAndFinalGazetteDraft() {
        Long boardId = DemoFixtures.mainBoardId(boardRepository);

        ScenarioSummaryDto summary = seeder.seedEndGame(boardId);

        assertTrue(summary.getMessage().contains("nursek"));
        Board board = boardRepository.findById(boardId).orElseThrow();
        assertTrue(board.isFinished());
        assertEquals("nursek",
                playerRepository.findById(board.getWinnerPlayerId()).orElseThrow().getName());
        List<Player> players = playerRepository.findByBoardId(boardId);
        assertEquals(1, players.stream().filter(player -> !player.isEliminated()).count());

        GamePodiumDto podium = gamePodiumService.getPodium(boardId);
        assertTrue(podium.isFinished());
        assertFalse(podium.isPosthumous());
        assertEquals("nursek", podium.getChampion().getPlayerName());
        assertEquals(players.size(), podium.getPlaces().size());

        Player winner = playerRepository.findById(board.getWinnerPlayerId()).orElseThrow();
        NotificationFeedDto feed = notificationService.getFeed(winner.getUserId(), boardId);
        assertTrue(feed.items().stream().anyMatch(notification ->
                        notification.type() == NotificationType.GAME_FINISHED
                                && "/podium".equals(notification.link())),
                "La fin de partie notifie le vainqueur vers le podium");

        GazetteIssue draft = gazetteIssueRepository
                .findByBoardIdAndTurn(boardId, board.getCurrentTurn())
                .orElseThrow();
        assertEquals(GazetteStatus.DRAFT, draft.getStatus());
        assertTrue(draft.isFinalEdition());

        ScenarioSummaryDto replay = seeder.seedEndGame(boardId);

        assertTrue(replay.getMessage().contains("déjà terminée"));
        assertTrue(gazetteIssueRepository
                .findByBoardIdAndTurn(boardId, board.getCurrentTurn())
                .isPresent());
    }
}
