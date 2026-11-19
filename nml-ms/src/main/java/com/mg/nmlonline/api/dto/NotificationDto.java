package com.mg.nmlonline.api.dto;

import com.mg.nmlonline.domain.model.notification.NotificationType;

import java.time.Instant;

public record NotificationDto(
        Long id,
        NotificationType type,
        String title,
        String message,
        String link,
        Instant createdAt,
        Instant readAt) {
}
