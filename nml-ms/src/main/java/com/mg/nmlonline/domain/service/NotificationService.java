package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.NotificationDto;
import com.mg.nmlonline.api.dto.NotificationFeedDto;
import com.mg.nmlonline.domain.model.notification.Notification;
import com.mg.nmlonline.domain.model.notification.NotificationType;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.repository.NotificationRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository repository;
    private final PlayerRepository playerRepository;

    public NotificationService(NotificationRepository repository,
                               PlayerRepository playerRepository) {
        this.repository = repository;
        this.playerRepository = playerRepository;
    }

    @Transactional(readOnly = true)
    public NotificationFeedDto getFeed(Long userId, Long boardId) {
        List<NotificationDto> items = repository
                .findTop100ByUserIdAndBoardIdOrderByCreatedAtDescIdDesc(userId, boardId).stream()
                .map(notification -> new NotificationDto(notification.getId(), notification.getType(),
                        notification.getTitle(), notification.getMessage(), notification.getLink(),
                        notification.getCreatedAt(), notification.getReadAt()))
                .toList();
        long unread = repository.countByUserIdAndBoardIdAndReadAtIsNull(userId, boardId);
        return new NotificationFeedDto(items, unread);
    }

    public void markRead(Long userId, Long boardId, Long notificationId) {
        Notification notification = owned(userId, boardId, notificationId);
        if (!notification.isRead()) {
            notification.setReadAt(Instant.now());
            repository.save(notification);
        }
    }

    public void markUnread(Long userId, Long boardId, Long notificationId) {
        Notification notification = owned(userId, boardId, notificationId);
        if (notification.isRead()) {
            notification.setReadAt(null);
            repository.save(notification);
        }
    }

    private Notification owned(Long userId, Long boardId, Long notificationId) {
        Notification notification = repository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Notification introuvable : " + notificationId));
        if (!boardId.equals(notification.getBoardId()) || !userId.equals(notification.getUserId())) {
            throw new SecurityException("Cette notification ne vous appartient pas.");
        }
        return notification;
    }

    public void markAllRead(Long userId, Long boardId) {
        repository.markAllRead(userId, boardId, Instant.now());
    }

    public void notify(Player recipient, NotificationType type, String title, String message,
                       String link) {
        if (recipient == null || recipient.getUserId() == null) {
            return;
        }
        repository.save(Notification.create(recipient.getBoardId(), recipient.getUserId(), type,
                title, message, link));
    }

    public void notifyPlayer(Long playerId, NotificationType type, String title, String message,
                             String link) {
        playerRepository.findById(playerId)
                .ifPresent(player -> notify(player, type, title, message, link));
    }

    public void notifyBoard(Long boardId, NotificationType type, String title, String message,
                            String link) {
        playerRepository.findByBoardId(boardId)
                .forEach(player -> notify(player, type, title, message, link));
    }

    public void notifyTurnEnded(Long boardId, int turnEnding, int newTurn, int captures,
                                List<String> eliminatedNames) {
        StringBuilder message = new StringBuilder("Le tour ").append(newTurn).append(" commence.");
        if (captures > 0) {
            message.append(' ').append(captures).append(" secteur(s) capturé(s).");
        }
        if (!eliminatedNames.isEmpty()) {
            message.append(' ').append(String.join(", ", eliminatedNames)).append(" éliminé(s).");
        }
        notifyBoard(boardId, NotificationType.TURN_ENDED, "Tour " + turnEnding + " terminé",
                message.toString(), "/rapports");
    }

    public void notifyBattleReports(int turn, Collection<Long> playerIds) {
        for (Long playerId : playerIds) {
            notifyPlayer(playerId, NotificationType.BATTLE_REPORTS_AVAILABLE,
                    "Rapports de combat disponibles",
                    "Vos combats du tour " + turn + " sont disponibles.", "/rapports");
        }
    }

    public void replaceGameFinished(Long boardId, Player winner) {
        repository.deleteByBoardIdAndType(boardId, NotificationType.GAME_FINISHED);
        notifyBoard(boardId, NotificationType.GAME_FINISHED, "Champion désigné",
                "Le MJ a désigné " + winner.getName() + " champion"
                        + (winner.isEliminated() ? " (victoire posthume)." : "."),
                "/podium");
    }
}
