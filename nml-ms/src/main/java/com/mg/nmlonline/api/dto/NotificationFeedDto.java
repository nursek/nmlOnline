package com.mg.nmlonline.api.dto;

import java.util.List;

public record NotificationFeedDto(List<NotificationDto> items, long unreadCount) {
}
