package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.EmbeddedPostgresTest;
import com.mg.nmlonline.api.dto.NotificationFeedDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.notification.NotificationType;
import com.mg.nmlonline.domain.model.player.EliminationCause;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@EmbeddedPostgresTest
@Transactional
@DisplayName("Notifications — fil, compteur non-lu et marquage")
class NotificationCenterTest {

    private static final AtomicLong USER_SEQ = new AtomicLong(900_000_000L);

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Test
    @DisplayName("le fil ne montre que le joueur visé et le compteur suit les lectures")
    void feedIsPerPlayerAndTracksReads() {
        Board board = board();
        Player first = player(board, "Premier");
        Player second = player(board, "Second");

        notificationService.notify(first, NotificationType.EXCHANGE_RECEIVED,
                "Nouvelle offre d'échange", "Une offre vous attend.", "/joueur");
        notificationService.notify(first, NotificationType.TURN_ENDED,
                "Tour 2 terminé", "Le tour 3 commence.", "/rapports");
        notificationService.notify(second, NotificationType.GAME_STARTED,
                "La partie commence", "Le tour 1 est lancé.", "/accueil");

        NotificationFeedDto before = notificationService.getFeed(first.getUserId(), board.getId());
        assertEquals(2, before.items().size());
        assertEquals(2, before.unreadCount());

        notificationService.markRead(first.getUserId(), board.getId(), before.items().getFirst().id());
        assertEquals(1, notificationService.getFeed(first.getUserId(), board.getId()).unreadCount());

        notificationService.markAllRead(first.getUserId(), board.getId());
        assertEquals(0, notificationService.getFeed(first.getUserId(), board.getId()).unreadCount());

        assertThrows(SecurityException.class, () -> notificationService.markRead(second.getUserId(),
                board.getId(), before.items().getFirst().id()));
    }

    @Test
    @DisplayName("une alerte de partie atteint tous les joueurs du plateau")
    void boardNotificationReachesEveryPlayer() {
        Board board = board();
        Player first = player(board, "Premier");
        Player second = player(board, "Second");

        notificationService.notifyBoard(board.getId(), NotificationType.GAZETTE_PUBLISHED,
                "Nouvelle édition du journal", "L'édition du tour 4 est publiée.", "/journal/4");

        assertEquals(1, notificationService.getFeed(first.getUserId(), board.getId()).unreadCount());
        assertEquals(1, notificationService.getFeed(second.getUserId(), board.getId()).unreadCount());
    }

    @Test
    @DisplayName("la désignation du champion remplace l'alerte de fin de partie")
    void replacingTheChampionReplacesTheEndGameAlert() {
        Board board = board();
        Player first = player(board, "Premier");
        Player second = player(board, "Second");
        second.eliminate(EliminationCause.MAIN_CHARACTER_KILLED, 5);

        notificationService.notifyBoard(board.getId(), NotificationType.GAME_FINISHED,
                "Partie terminée", "Victoire de Premier.", "/podium");
        notificationService.markAllRead(first.getUserId(), board.getId());

        notificationService.replaceGameFinished(board.getId(), second);

        NotificationFeedDto feed = notificationService.getFeed(first.getUserId(), board.getId());
        assertEquals(1, feed.items().size());
        assertEquals(NotificationType.GAME_FINISHED, feed.items().getFirst().type());
        assertEquals("Champion désigné", feed.items().getFirst().title());
        assertEquals("/podium", feed.items().getFirst().link());
        assertEquals(1, feed.unreadCount());
    }

    private Board board() {
        Board board = new Board();
        board.setName("Notifications " + UUID.randomUUID());
        return boardRepository.save(board);
    }

    private Player player(Board board, String name) {
        Player player = new Player(name);
        player.setBoardId(board.getId());
        player.setUserId(USER_SEQ.incrementAndGet());
        return playerRepository.save(player);
    }
}
