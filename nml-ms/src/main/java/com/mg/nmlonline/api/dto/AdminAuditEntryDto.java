package com.mg.nmlonline.api.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class AdminAuditEntryDto {
    private Long id;
    private String adminUsername;
    private Long playerId;
    private String action;
    private String details;
    private Instant createdAt;
    private Integer turn;
}
