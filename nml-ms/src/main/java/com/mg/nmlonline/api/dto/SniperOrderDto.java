package com.mg.nmlonline.api.dto;

import lombok.Data;

@Data
public class SniperOrderDto {
    private Long id;
    private int turn;
    private Long playerId;
    private Long unitId;
    private int fromSectorNumber;
    private int targetSectorNumber;
    private String status;
    private String statusMessage;
    private String action;
}
