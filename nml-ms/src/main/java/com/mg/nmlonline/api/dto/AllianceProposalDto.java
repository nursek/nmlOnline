package com.mg.nmlonline.api.dto;

import lombok.Data;

@Data
public class AllianceProposalDto {
    private Long id;
    private String kind;
    private String status;
    private Long fromPlayerId;
    private String fromPlayerName;
    private Long toPlayerId;
    private String toPlayerName;
    private int createdTurn;
    /** IN = reçue, OUT = envoyée (du point de vue du joueur courant). */
    private String direction;
}
