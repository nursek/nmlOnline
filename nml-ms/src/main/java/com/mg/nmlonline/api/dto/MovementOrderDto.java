package com.mg.nmlonline.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class MovementOrderDto {
    private Long id;
    private int turn;
    private Long playerId;
    private String status;
    private int fromSectorNumber;
    private int toSectorNumber;
    private List<Integer> route;
    private List<Long> entityIds;
    private Long vehicleId;
    private String statusMessage;
}