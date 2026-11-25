package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.NotificationFeedDto;
import com.mg.nmlonline.config.BoardScopeFilter;
import com.mg.nmlonline.domain.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<NotificationFeedDto> getFeed(HttpServletRequest request) {
        return ResponseEntity.ok(notificationService.getFeed(userId(request), boardId(request)));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable("id") Long id,
                                         HttpServletRequest request) {
        notificationService.markRead(userId(request), boardId(request), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/unread")
    public ResponseEntity<Void> markUnread(@PathVariable("id") Long id,
                                           HttpServletRequest request) {
        notificationService.markUnread(userId(request), boardId(request), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(HttpServletRequest request) {
        notificationService.markAllRead(userId(request), boardId(request));
        return ResponseEntity.noContent().build();
    }

    private Long userId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new SecurityException("Utilisateur non authentifié.");
        }
        return userId;
    }

    private Long boardId(HttpServletRequest request) {
        return (Long) request.getAttribute(BoardScopeFilter.BOARD_ID_ATTRIBUTE);
    }
}
