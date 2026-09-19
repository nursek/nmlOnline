package com.mg.nmlonline.api.dto;

import lombok.Data;

@Data
public class AnnouncementDto {
    private Long id;
    private String type;
    private Long actorPlayerId;
    private String actorName;
    private Long targetPlayerId;
    private String targetName;
    private int turnCreated;
    private int visibleAtTurn;
}
