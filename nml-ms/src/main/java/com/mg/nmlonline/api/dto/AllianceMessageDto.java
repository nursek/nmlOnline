package com.mg.nmlonline.api.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class AllianceMessageDto {
    private Long id;
    private Long senderPlayerId;
    private String senderName;
    private String body;
    private int turn;
    private Instant createdAt;
}
