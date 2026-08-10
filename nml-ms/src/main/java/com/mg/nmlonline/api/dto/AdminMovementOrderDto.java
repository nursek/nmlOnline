package com.mg.nmlonline.api.dto;

import lombok.Data;

import java.util.List;

/**
 * DTO d'un ordre de déplacement exposé à l'admin : {@link MovementOrderDto}
 * enrichi du nom du joueur (lookup batch côté service pour éviter le N+1).
 */
@Data
public class AdminMovementOrderDto {
    private Long id;
    private int turn;
    private Long playerId;
    private String playerName;
    private String status;
    private int fromSectorNumber;
    private int toSectorNumber;
    private List<Integer> route;
    private List<Long> entityIds;
    private Long vehicleId;
    private String statusMessage;
}