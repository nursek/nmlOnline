package com.mg.nmlonline.api.dto;

import lombok.Data;

/**
 * Résultat de la finalisation d'une session pas-à-pas : nouveau numéro de
 * tour et compte-rendu synthétique (ordres résolus/bloqués, batailles résolues,
 * combats de transit).
 */
@Data
public class TurnFinalizeResultDto {
    private int newTurn;
    private int turnEnding;
    private int resolvedOrders;
    private int blockedOrders;
    private int conflictsResolved;
    private int transitCombats;
    private String message;
}